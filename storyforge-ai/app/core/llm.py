"""LLM 클라이언트 추상화. 서비스 코드는 공급자를 직접 알지 않습니다."""
from __future__ import annotations

from collections.abc import AsyncGenerator
from typing import Protocol, TypeVar

import anthropic
from pydantic import BaseModel

from app.core.config import Settings
from app.core.ollama import LLMError

T = TypeVar("T", bound=BaseModel)

# 정책상 거절(refusal) 시 서버가 적절한 모델로 다시 실행합니다.
FALLBACK_BETA = "server-side-fallback-2026-07-01"


class LLMClient(Protocol):
    is_mock: bool

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T: ...

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str: ...

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncGenerator[str, None]: ...


class MockLLM:
    """API 키 없이 파이프라인을 확인하기 위한 표시용 클라이언트. 생성 내용은 services/mock_writer.py가 만듭니다."""

    is_mock = True

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncGenerator[str, None]:
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

    async def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncGenerator[str, None]:
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
