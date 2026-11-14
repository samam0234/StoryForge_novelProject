"""v1 내부 API의 경로·그룹을 조립합니다. 개별 파일은 HTTP 입출력만 담당합니다."""
from fastapi import APIRouter

from app.api.v1 import bible, chapter, guide, live, outline, package


def create_router() -> APIRouter:
    router = APIRouter()
    generation = APIRouter(prefix="/internal/generate", tags=["internal"])
    for route in (bible.router, outline.router, chapter.router):
        generation.include_router(route)
    router.include_router(generation)
    router.include_router(guide.router)
    router.include_router(package.router)
    router.include_router(live.router)
    return router
