"""v1 내부 라우터. Spring Boot만 호출합니다 (기획서 §7.2: /internal/generate/*)."""
from fastapi import APIRouter

from app.api.v1 import bible, chapter, outline

router = APIRouter(prefix="/internal/generate", tags=["internal"])
router.include_router(bible.router)
router.include_router(outline.router)
router.include_router(chapter.router)
