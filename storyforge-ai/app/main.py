"""StoryForge AI Service — 내부망 전용 (기획서 §7.3). 외부(브라우저)에 노출하지 않습니다."""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.v1 import guide, package
from app.api.v1 import router as v1_router
from app.core.config import get_settings
from app.core.llm import LLMError, RoutingLLM, get_llm
from app.core.ollama import LLMBusyError

logging.basicConfig(level=logging.INFO)

@asynccontextmanager
async def lifespan(_: FastAPI):
    llm = get_llm()
    try:
        if isinstance(llm, RoutingLLM) and get_settings().llm_provider == "ollama" and get_settings().ollama_warmup:
            try:
                await llm.warmup()
            except Exception:
                logging.getLogger(__name__).warning("Ollama 예열 실패. 첫 요청에서 다시 연결합니다.", exc_info=True)
        yield
    finally:
        if isinstance(llm, RoutingLLM):
            await llm.aclose()
        get_llm.cache_clear()


app = FastAPI(title="StoryForge AI Service", version="0.2.0", lifespan=lifespan)
app.include_router(v1_router)
app.include_router(guide.router)
app.include_router(package.router)


@app.exception_handler(LLMError)
async def llm_error(_: Request, e: LLMError) -> JSONResponse:
    return JSONResponse(status_code=503 if isinstance(e, LLMBusyError) else 502,
                        content={"detail": str(e)},
                        headers={"Retry-After": "5"} if isinstance(e, LLMBusyError) else None)


@app.get("/health")
async def health() -> dict[str, str]:
    s = get_settings()
    model = s.ollama_model if s.llm_provider == "ollama" else s.llm_model
    return {"status": "ok", "provider": s.llm_provider, "model": model if s.llm_provider != "mock" else "-"}
