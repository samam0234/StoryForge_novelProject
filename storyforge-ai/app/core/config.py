"""환경 변수 기반 설정. 기준값은 docs/plan/CURRENT_STACK.md."""
from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=(".env", "../.env"), extra="ignore")

    # mock: API 키 없이 동작하는 개발용 생성기 / anthropic: Claude API
    llm_provider: Literal["mock", "ollama", "anthropic"] = "mock"
    llm_model: str = "claude-opus-5-5"
    ollama_base_url: str = "http://127.0.0.1:11434"
    ollama_model: str = "gemma4:e4b"
    # Claude Opus 5.5의 기본 effort는 medium입니다. 소설 생성은 추론을 더 쓰도록 high로 올렸습니다.
    llm_effort: Literal["low", "medium", "high", "xhigh", "max"] = "high"
    # 비워 두면 SDK가 ANTHROPIC_API_KEY 등 표준 경로에서 자격 증명을 찾습니다.
    anthropic_api_key: str | None = None

    # mock 스트리밍 속도 (초/조각)
    mock_stream_delay: float = 0.02


@lru_cache
def get_settings() -> Settings:
    return Settings()
