"""실제 모델 없이 연결 재사용, 구조화 응답, 대기 제한, 스트림 오류를 검증합니다."""
import asyncio
import json

import httpx
import pytest
from pydantic import BaseModel, Field, ValidationError
from pydantic_settings import SettingsConfigDict

from app.core.config import Settings
from app.core.llm import RoutingLLM
from app.core.ollama import LLMBusyError, LLMError, OllamaLLM


class Result(BaseModel):
    world_setting: str = Field(alias="worldSetting")


class IsolatedSettings(Settings):
    model_config = SettingsConfigDict(env_file=None, extra="ignore")


def settings(**kwargs):
    return IsolatedSettings(**kwargs)


def test_pool_payload_and_structured_schema():
    requests = []

    def handler(request):
        requests.append(json.loads(request.content))
        return httpx.Response(200, json={"done": True, "message": {"content": '{"worldSetting":"설정"}'}})

    async def run():
        llm = OllamaLLM(settings(), transport=httpx.MockTransport(handler))
        try:
            result = await llm.structured(system="규칙", prompt="성경과 아웃라인", schema=Result)
            client = llm._http()
            await llm.text(system="규칙", prompt="본문", max_tokens=250)
            assert client is llm._http()
            assert result.world_setting == "설정"
        finally:
            await llm.aclose()
        assert client.is_closed

    asyncio.run(run())
    payload = requests[0]
    assert payload["format"]["required"] == ["worldSetting"]
    assert payload["keep_alive"] == "15m"
    assert payload["think"] is False
    assert payload["options"]["num_ctx"] == 32768
    assert payload["messages"][1]["content"] == "성경과 아웃라인"
    assert requests[1]["options"]["num_predict"] == 250


@pytest.mark.parametrize("body", [
    {"done": True, "message": {"content": "{}"}},
    {"done": True, "message": {"content": '설명 {"worldSetting":"설정"}'}},
    {"done": True, "message": {"content": ""}},
    {"done": True, "done_reason": "length", "message": {"content": '{"worldSetting":"설정"}'}},
    {"message": {"content": '{"worldSetting":"설정"}'}},
    {"error": "server error"},
    [],
])
def test_invalid_or_incomplete_output_is_not_success(body):
    async def run():
        llm = OllamaLLM(settings(), transport=httpx.MockTransport(lambda _: httpx.Response(200, json=body)))
        try:
            with pytest.raises(LLMError):
                await llm.structured(system="s", prompt="p", schema=Result)
        finally:
            await llm.aclose()
    asyncio.run(run())


@pytest.mark.parametrize("ending", [None, {"error": "broken"}, {"done": True, "done_reason": "length"}])
def test_partial_stream_is_followed_by_error(ending):
    text = json.dumps({"message": {"content": "일부 본문"}}, ensure_ascii=False) + "\n"
    if ending is not None:
        text += json.dumps(ending) + "\n"

    async def run():
        llm = OllamaLLM(settings(), transport=httpx.MockTransport(lambda _: httpx.Response(200, text=text)))
        received = []
        try:
            with pytest.raises(LLMError):
                async for chunk in llm.stream(system="s", prompt="p"):
                    received.append(chunk)
            assert received == ["일부 본문"]
            # 오류 후에도 슬롯을 반환합니다.
            assert llm._slots._value == 1
        finally:
            await llm.aclose()
    asyncio.run(run())


def test_stream_finishes_and_cancellation_releases_capacity():
    async def run():
        llm = OllamaLLM(settings(), transport=httpx.MockTransport(lambda _: httpx.Response(200, text=
            '{"message":{"content":"본문"}}\n{"done":true,"done_reason":"stop"}\n')))
        try:
            stream = llm.stream(system="s", prompt="p")
            assert await stream.__anext__() == "본문"
            await stream.aclose()
            assert llm._slots._value == 1
            assert [part async for part in llm.stream(system="s", prompt="p")] == ["본문"]
        finally:
            await llm.aclose()
    asyncio.run(run())


def test_concurrent_request_wait_timeout_and_task_cancellation():
    async def run():
        entered = asyncio.Event()
        finish = asyncio.Event()

        async def handler(_):
            entered.set()
            await finish.wait()
            return httpx.Response(200, json={"done": True, "message": {"content": "응답"}})

        llm = OllamaLLM(settings(ollama_queue_timeout=0.02), transport=httpx.MockTransport(handler))
        try:
            first = asyncio.create_task(llm.text(system="s", prompt="p"))
            await entered.wait()
            with pytest.raises(LLMBusyError):
                await llm.text(system="s", prompt="p")
            first.cancel()
            with pytest.raises(asyncio.CancelledError):
                await first
            finish.set()
            assert await llm.text(system="s", prompt="p") == "응답"
        finally:
            await llm.aclose()
    asyncio.run(run())


def test_routing_does_not_require_claude_credentials_for_ollama(monkeypatch):
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    llm = RoutingLLM(settings(llm_provider="ollama"))
    assert isinstance(llm._pick(None), OllamaLLM)
    assert llm._claude is None


@pytest.mark.parametrize("kwargs", [{"ollama_max_concurrent": 0}, {"ollama_queue_timeout": 0}, {"ollama_stream_max_tokens": 1}])
def test_settings_reject_invalid_capacity(kwargs):
    with pytest.raises(ValidationError):
        settings(**kwargs)


def test_whitespace_only_stream_is_not_success():
    async def run():
        llm = OllamaLLM(settings(), transport=httpx.MockTransport(lambda _: httpx.Response(200, text=
            '{"message":{"content":" "}}\n{"message":{"content":"\\n"}}\n{"done":true}\n')))
        try:
            with pytest.raises(LLMError, match="빈 응답"):
                async for _ in llm.stream(system="s", prompt="p"):
                    pass
            assert llm._slots._value == 1
        finally:
            await llm.aclose()
    asyncio.run(run())
