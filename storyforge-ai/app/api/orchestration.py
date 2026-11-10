"""HTTP 계층의 라우터와 AI 오류 응답 계약을 등록합니다."""
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.v1.orchestration import create_router
from app.core.llm import LLMError
from app.core.ollama import LLMBusyError


async def llm_error(_: Request, error: Exception) -> JSONResponse:
    busy = isinstance(error, LLMBusyError)
    return JSONResponse(status_code=503 if busy else 502, content={"detail": str(error)},
                        headers={"Retry-After": "5"} if busy else None)


def register_api(application: FastAPI) -> None:
    application.include_router(create_router())
    application.add_exception_handler(LLMError, llm_error)
