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

    async def structured(self, *, system: str, prompt: str, schema: type[T]) -> T: ...

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000) -> str: ...

    def stream(self, *, system: str, prompt: str) -> AsyncIterator[str]: ...


class MockLLM:
    """API 키 없이 파이프라인을 확인하기 위한 표시용 클라이언트. 생성 내용은 services/mock_writer.py가 만듭니다."""

    is_mock = True

    async def structured(self, *, system: str, prompt: str, schema: type[T]) -> T:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000) -> str:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")

    def stream(self, *, system: str, prompt: str) -> AsyncIterator[str]:
        raise NotImplementedError("mock 모드에서는 mock_writer를 사용합니다.")


class AnthropicLLM:
    is_mock = False

    def __init__(self, settings: Settings) -> None:
        self._client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key or None)
        self._common = {
            "model": settings.llm_model,
            "betas": [FALLBACK_BETA],
            "fallbacks": "default",
            "output_config": {"effort": settings.llm_effort},
        }

    async def structured(self, *, system: str, prompt: str, schema: type[T]) -> T:
        try:
            response = await self._client.beta.messages.parse(
                max_tokens=16000,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                output_format=schema,
                **self._common,
            )
        except anthropic.APIError as e:
            raise LLMError(f"Claude API 호출 실패: {e}") from e
        _check_stop(response.stop_reason)
        if response.parsed_output is None:
            raise LLMError("구조화 응답을 해석하지 못했습니다.")
        return response.parsed_output

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000) -> str:
        try:
            response = await self._client.beta.messages.create(
                max_tokens=max_tokens,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                **self._common,
            )
        except anthropic.APIError as e:
            raise LLMError(f"Claude API 호출 실패: {e}") from e
        _check_stop(response.stop_reason)
        return "".join(b.text for b in response.content if b.type == "text")

    async def stream(self, *, system: str, prompt: str) -> AsyncIterator[str]:
        try:
            async with self._client.beta.messages.stream(
                max_tokens=64000,
                system=system,
                messages=[{"role": "user", "content": prompt}],
                **self._common,
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


@lru_cache
def get_llm() -> LLMClient:
    settings = get_settings()
    if settings.llm_provider == "anthropic":
        return AnthropicLLM(settings)
    return MockLLM()
