"""모델 공급자 선택과 프로세스 단위 클라이언트의 시작·예열·종료를 관리합니다."""
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from functools import lru_cache
import logging

from pydantic import BaseModel
from typing import TypeVar

from app.core.config import Settings, get_settings
from app.core.llm import AnthropicLLM, LLMClient, MockLLM
from app.core.ollama import OllamaLLM

T = TypeVar("T", bound=BaseModel)


class RoutingLLM:
    """프로젝트 모델 값으로 일반(Ollama)과 강력 출력(Claude)을 나눕니다."""

    is_mock = False

    def __init__(self, settings: Settings) -> None:
        self._ollama = OllamaLLM(settings)
        self._claude: AnthropicLLM | None = None
        self._settings = settings

    def _pick(self, model: str | None) -> OllamaLLM | AnthropicLLM:
        name = model or (self._settings.llm_model if self._settings.llm_provider == "anthropic" else "")
        if name.startswith("claude"):
            if self._claude is None:
                self._claude = AnthropicLLM(self._settings)
            return self._claude
        return self._ollama

    async def structured(self, *, system: str, prompt: str, schema: type[T], model: str | None = None) -> T:
        return await self._pick(model).structured(system=system, prompt=prompt, schema=schema, model=model)

    async def text(self, *, system: str, prompt: str, max_tokens: int = 4000, model: str | None = None) -> str:
        return await self._pick(model).text(system=system, prompt=prompt, max_tokens=max_tokens, model=model)

    def stream(self, *, system: str, prompt: str, model: str | None = None) -> AsyncGenerator[str, None]:
        return self._pick(model).stream(system=system, prompt=prompt, model=model)


    async def warmup(self) -> None:
        await self._ollama.warmup()

    async def aclose(self) -> None:
        await self._ollama.aclose()
        if self._claude is not None:
            await self._claude._client.close()


@lru_cache
def get_llm() -> LLMClient:
    settings = get_settings()
    if settings.llm_provider == "mock":
        return MockLLM()
    return RoutingLLM(settings)


@asynccontextmanager
async def llm_runtime():
    llm = get_llm()
    try:
        settings = get_settings()
        if isinstance(llm, RoutingLLM) and settings.llm_provider == "ollama" and settings.ollama_warmup:
            try:
                await llm.warmup()
            except Exception:
                logging.getLogger(__name__).warning("Ollama 예열 실패. 첫 요청에서 다시 연결합니다.", exc_info=True)
        yield
    finally:
        try:
            if isinstance(llm, RoutingLLM):
                await llm.aclose()
        finally:
            get_llm.cache_clear()


def runtime_status() -> dict[str, str]:
    settings = get_settings()
    model = settings.ollama_model if settings.llm_provider == "ollama" else settings.llm_model
    return {"status": "ok", "provider": settings.llm_provider,
            "model": model if settings.llm_provider != "mock" else "-"}
