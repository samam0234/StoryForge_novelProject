"""StoryForge AI Service — 내부망 전용 (기획서 §7.3). 외부(브라우저)에 노출하지 않습니다."""
import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.v1 import router as v1_router
from app.core.config import get_settings
from app.core.llm import LLMError

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="StoryForge AI Service", version="0.1.0")
app.include_router(v1_router)


@app.exception_handler(LLMError)
async def llm_error(_: Request, e: LLMError) -> JSONResponse:
    return JSONResponse(status_code=502, content={"detail": str(e)})


@app.get("/health")
async def health() -> dict[str, str]:
    s = get_settings()
    return {"status": "ok", "provider": s.llm_provider, "model": s.llm_model if s.llm_provider != "mock" else "-"}
