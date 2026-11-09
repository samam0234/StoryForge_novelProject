"""LLM 클라이언트 추상화. 서비스 코드는 공급자를 직접 알지 않습니다."""
from __future__ import annotations

from collections.abc import AsyncIterator
from functools import lru_cache
from typing import Protocol, TypeVar

import anthropic
from pydantic import BaseModel

from app.core.config import Settings, get_settings

T = TypeVar("T", bound=BaseModel)

# 정책상 거절(refusal) 시 서버가 적절한 모델로 다시 실행합니다.
FALLBACK_BETA = "server-side-fallback-2026-07-01"


class LLMError(Exception):
    """LLM 호출 실패. API 라우터에서 502로 변환합니다."""


class LLMClient(Protocol):
    is_mock: bool

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T: ...

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str: ...

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncIterator[str]: ...


class MockLLM:
    """API 키 없이 파이프라인을 확인하기 위한 표시용 클라이언트. 생성 내용은 services/mock_writer.py가 만듭니다."""

    is_mock = True

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncIterator[str]:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")


class AnthropicLLM:
    is_mock = False

    def __init__(self, settings: Settings) -> None:
        self._client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key or None)
        self._default_model = settings.llm_model
        self._effort = settings.llm_effort

    def _params(self, model: str | None) -> dict:
        name = model or self._default_model
        effort = _EFFORT.get(name, self._effort)
        params: dict = {"model": name, "output_config": {"effort": effort}}
        # Haiku에는 서버 측 refusal fallback이 없습니다.
        if not name.startswith("claude-haiku"):
            params |= {"betas": [FALLBACK_BETA], "fallbacks": "default"}
        return params

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        try:
            response = await self._client.beta.messages.parse(
                max_tokens=16000,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                output_format=schema,
                **self._params(model),
            )
        except anthropic.APIError as e:
            raise LLMError(f"Claude API 호출 실패: {e}") from e
        _check_stop(response.stop_reason)
        if response.parsed_output is None:
            raise LLMError("구조화 응답을 해석하지 못했습니다.")
        return response.parsed_output

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        try:
            response = await self._client.beta.messages.create(
                max_tokens=max_tokens,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                **self._params(model),
            )
        except anthropic.APIError as e:
            raise LLMError(f"Claude API 호출 실패: {e}") from e
        _check_stop(response.stop_reason)
        return "".join(b.text for b in response.content if b.type == "text")

    async def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncIterator[str]:
        try:
            async with self._client.beta.messages.stream(
                max_tokens=64000,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                **self._params(model),
            ) as s:
                async for chunk in s.text_stream:
                    yield chunk
                final = await s.get_final_message()
        except anthropic.APIError as e:
            raise LLMError(f"Claude API 스트리밍 실패: {e}") from e
        _check_stop(final.stop_reason)


def _check_stop(stop_reason: str | None) -> None:
    if stop_reason == "refusal":
        raise LLMError("모델이 요청을 거절했습니다.")
    if stop_reason == "max_tokens":
        raise LLMError("출력 한도에 도달해 응답이 잘렸습니다.")


# 강력 출력에서 고른 모델의 추론 강도. 일반(Gemma)은 이 표를 쓰지 않습니다.
_EFFORT = {
    "claude-haiku-5-5": "high",
    "claude-sonnet-5-5": "medium",
    "claude-opus-5-5": "medium",
}


class OllamaLLM:
    """일반 출력. 이 PC의 Ollama Gemma 4 E4B."""

    is_mock = False

    def __init__(self, settings: Settings) -> None:
        self._base = settings.ollama_base_url.rstrip("/")
        self._model = settings.ollama_model

    def _name(self, model: str | None) -> str:
        return model or self._model

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        raw = await self.text(
            system=system + "\nJSON만 출력합니다.",
            prompt=prompt,
            max_tokens=8000,
            model=model,
        )
        start = raw.find("{")
        end = raw.rfind("}")
        if start < 0 or end < start:
            raise LLMError("Ollama 응답에서 JSON을 찾지 못했습니다.")
        try:
            return schema.model_validate_json(raw[start : end + 1])
        except Exception as e:
            raise LLMError("Ollama JSON을 해석하지 못했습니다.") from e

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        import httpx

        payload = {
            "model": self._name(model),
            "stream": False,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": prompt},
            ],
            "options": {"num_predict": max_tokens},
        }
        try:
            async with httpx.AsyncClient(timeout=180) as client:
                response = await client.post(f"{self._base}/api/chat", json=payload)
                response.raise_for_status()
                body = response.json()
        except httpx.HTTPError as e:
            raise LLMError(f"Ollama 호출 실패: {e}") from e
        return body.get("message", {}).get("content", "")

    async def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncIterator[str]:
        import httpx

        payload = {
            "model": self._name(model),
            "stream": True,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": prompt},
            ],
        }
        try:
            async with httpx.AsyncClient(timeout=180) as client:
                async with client.stream("POST", f"{self._base}/api/chat", json=payload) as response:
                    response.raise_for_status()
                    async for line in response.aiter_lines():
                        if not line:
                            continue
                        import json

                        chunk = json.loads(line)
                        text = chunk.get("message", {}).get("content", "")
                        if text:
                            yield text
        except httpx.HTTPError as e:
            raise LLMError(f"Ollama 스트리밍 실패: {e}") from e


class RoutingLLM:
    """프로젝트 모델 값으로 일반(Ollama)과 강력 출력(Claude)을 나눕니다."""

    is_mock = False

    def __init__(self, settings: Settings) -> None:
        self._ollama = OllamaLLM(settings)
        self._claude = AnthropicLLM(settings)

    def _pick(self, model: str | None) -> OllamaLLM | AnthropicLLM:
        name = model or ""
        return self._claude if name.startswith("claude") else self._ollama

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        return await self._pick(model).structured(system=system, prompt=prompt, schema=schema, model=model)

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        return await self._pick(model).text(system=system, prompt=prompt, max_tokens=max_tokens, model=model)

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncIterator[str]:
        return self._pick(model).stream(system=system, prompt=prompt, model=model)


@lru_cache
def get_llm() -> LLMClient:
    settings = get_settings()
    if settings.llm_provider == "mock":
        return MockLLM()
    return RoutingLLM(settings)
