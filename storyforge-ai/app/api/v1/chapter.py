"""챕터 생성 SSE. 이벤트: token(본문 조각) → done(ChapterGenerateResult JSON) | error(메시지)"""
import logging
from collections.abc import AsyncIterator
from contextlib import aclosing

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.core.llm import LLMError
from app.api.dependencies import get_tasks
from app.services.orchestration import AiTaskOrchestrator
from app.models.schemas import ChapterContext, ChapterReviewRequest, ChapterReviewResult, CompleteRequest, CompleteResponse
from app.models.orchestration import Done, Token

router = APIRouter()
log = logging.getLogger(__name__)


def sse(event: str, data: str) -> str:
    # 여러 줄 데이터는 줄마다 data: 를 붙입니다 (수신 측에서 \n으로 다시 합칩니다).
    lines = "\n".join(f"data: {line}" for line in data.split("\n"))
    return f"event: {event}\n{lines}\n\n"


@router.post("/complete", response_model=CompleteResponse, response_model_by_alias=True)
async def complete(request: CompleteRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> CompleteResponse:
    suggestion = await tasks.complete(request.before, request.after, request.tone)
    return CompleteResponse(suggestion=suggestion)


@router.post("/review", response_model=ChapterReviewResult, response_model_by_alias=True)
async def review(request: ChapterReviewRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> ChapterReviewResult:
    return await tasks.review_chapter(request)


@router.post("/chapter")
async def chapter(ctx: ChapterContext, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> StreamingResponse:
    async def events() -> AsyncIterator[str]:
        try:
            async with aclosing(tasks.generate_chapter(ctx)) as generation:
                async for item in generation:
                    if isinstance(item, Token):
                        yield sse("token", item.text)
                    elif isinstance(item, Done):
                        yield sse("done", item.result.model_dump_json(by_alias=True))
        except LLMError as e:
            log.warning("chapter generation failed: %s", e)
            yield sse("error", str(e))
        except Exception:
            log.exception("unexpected chapter generation error")
            yield sse("error", "AI 서비스 내부 오류")

    return StreamingResponse(events(), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})
