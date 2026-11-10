"""Ollama 연결 풀, 과부하 보호와 검증. 컨텍스트는 호출자가 전달한 그대로 유지합니다."""
from __future__ import annotations

import asyncio
import json
import logging
import time
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from typing import TypeVar

import httpx
from pydantic import BaseModel, ValidationError

from app.core.config import Settings

T = TypeVar("T", bound=BaseModel)
log = logging.getLogger(__name__)


class LLMError(Exception):
    """공급자 실패. 부분 출력을 정상 결과로 저장하지 않습니다."""


class LLMBusyError(LLMError):
    """설정된 대기 시간 내에 추론 슬롯을 얻지 못했습니다."""


class OllamaLLM:
    is_mock = False

    def __init__(self, settings: Settings, *, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._settings = settings
        self._transport = transport
        self._client: httpx.AsyncClient | None = None
        self._slots = asyncio.Semaphore(settings.ollama_max_concurrent)

    def _http(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self._settings.ollama_base_url.rstrip("/"),
                timeout=httpx.Timeout(self._settings.ollama_timeout, connect=5, pool=5),
                limits=httpx.Limits(max_connections=self._settings.ollama_max_concurrent,
                                   max_keepalive_connections=self._settings.ollama_max_concurrent,
                                   keepalive_expiry=60),
                transport=self._transport,
            )
        return self._client

    @asynccontextmanager
    async def _slot(self):
        try:
            await asyncio.wait_for(self._slots.acquire(), timeout=self._settings.ollama_queue_timeout)
        except asyncio.TimeoutError as exc:
            raise LLMBusyError("AI 서버가 다른 요청을 처리 중입니다. 잠시 후 다시 시도하세요.") from exc
        try:
            yield
        finally:
            self._slots.release()

    def _payload(self, system: str, prompt: str, model: str | None, *, stream: bool, max_tokens: int) -> dict:
        return {
            "model": model or self._settings.ollama_model,
            "stream": stream,
            "think": self._settings.ollama_think,
            "keep_alive": self._settings.ollama_keep_alive,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            "options": {"num_predict": max_tokens, "num_ctx": self._settings.ollama_num_ctx},
        }

    @staticmethod
    def _content(body: dict, *, terminal: bool = False) -> str:
        if body.get("error"):
            raise LLMError("Ollama가 생성 오류를 반환했습니다.")
        if body.get("done_reason") == "length":
            raise LLMError("출력 한도에 도달해 응답이 잘렸습니다.")
        if terminal and body.get("done") is not True:
            raise LLMError("Ollama 응답이 완료되지 않았습니다.")
        content = body.get("message", {}).get("content", "")
        if not isinstance(content, str):
            raise LLMError("Ollama 응답 형식이 올바르지 않습니다.")
        return content

    @staticmethod
    def _timing(body: dict, elapsed: float, first_token: float | None = None) -> None:
        # 원문과 사용자 정보는 기록하지 않습니다. Ollama duration 단위는 ns입니다.
        log.info("ollama completed elapsed_s=%.3f first_token_s=%s load_ns=%s prompt_tokens=%s output_tokens=%s eval_ns=%s",
                 elapsed, first_token, body.get("load_duration"), body.get("prompt_eval_count"),
                 body.get("eval_count"), body.get("eval_duration"))

    async def _request(self, payload: dict) -> str:
        started = time.perf_counter()
        try:
            async with self._slot():
                response = await self._http().post("/api/chat", json=payload)
                response.raise_for_status()
                body = response.json()
                content = self._content(body, terminal=True)
                if not content.strip():
                    raise LLMError("Ollama가 빈 응답을 반환했습니다.")
                self._timing(body, time.perf_counter() - started)
                return content
        except (httpx.HTTPError, ValueError, TypeError, AttributeError) as exc:
            raise LLMError("Ollama 응답을 받지 못했습니다. 서버 상태를 확인하세요.") from exc

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        json_schema = schema.model_json_schema(by_alias=True)
        payload = self._payload(system + "\n아래 JSON 스키마에 맞는 JSON만 출력합니다.\n" + json.dumps(json_schema, ensure_ascii=False),
                                prompt, model, stream=False, max_tokens=self._settings.ollama_structured_max_tokens)
        payload["format"] = json_schema
        payload["options"]["temperature"] = 0
        raw = await self._request(payload)
        try:
            return schema.model_validate_json(raw)
        except ValidationError as exc:
            raise LLMError("Ollama JSON이 요청한 구조와 일치하지 않습니다.") from exc

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        return await self._request(self._payload(system, prompt, model, stream=False, max_tokens=max_tokens))

    async def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncGenerator[str, None]:
        payload = self._payload(system, prompt, model, stream=True, max_tokens=self._settings.ollama_stream_max_tokens)
        started = time.perf_counter()
        first_token = None
        received_content = False
        try:
            async with self._slot():
                async with self._http().stream("POST", "/api/chat", json=payload) as response:
                    response.raise_for_status()
                    async for line in response.aiter_lines():
                        if not line:
                            continue
                        chunk = json.loads(line)
                        if isinstance(chunk, dict) and chunk.get("done") is True:
                            self._timing(chunk, time.perf_counter() - started, first_token)
                        content = self._content(chunk)
                        if content:
                            if first_token is None:
                                first_token = time.perf_counter() - started
                            received_content = received_content or bool(content.strip())
                            yield content
                        if chunk.get("done") is True:
                            if not received_content:
                                raise LLMError("Ollama가 빈 응답을 반환했습니다.")
                            return
                    raise LLMError("Ollama 스트림이 완료 신호 없이 종료되었습니다.")
        except (httpx.HTTPError, ValueError, TypeError, AttributeError) as exc:
            raise LLMError("Ollama 스트리밍 응답을 받지 못했습니다.") from exc

    async def warmup(self) -> None:
        async with self._slot():
            response = await self._http().post("/api/generate", json={
                "model": self._settings.ollama_model, "keep_alive": self._settings.ollama_keep_alive, "stream": False,
                "options": {"num_ctx": self._settings.ollama_num_ctx},
            })
            response.raise_for_status()
            body = response.json()
            if body.get("error") or body.get("done") is not True:
                raise LLMError("Ollama 모델 예열에 실패했습니다.")

    async def aclose(self) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None
