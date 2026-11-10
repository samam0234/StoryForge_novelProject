"""AI 서버 조립. 모델 수명주기와 API를 연결하며 프로젝트 상태는 관리하지 않습니다."""
from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI

from app.api.orchestration import register_api
from app.core.orchestration import llm_runtime, runtime_status


@asynccontextmanager
async def lifespan(_: FastAPI):
    async with llm_runtime():
        yield


def create_app() -> FastAPI:
    logging.basicConfig(level=logging.INFO)
    application = FastAPI(title="StoryForge AI Service", version="0.2.0", lifespan=lifespan)
    register_api(application)

    @application.get("/health")
    async def health() -> dict[str, str]:
        return runtime_status()

    return application
