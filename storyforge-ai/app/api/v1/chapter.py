"""챕터 생성 SSE. 이벤트: token(본문 조각) → done(ChapterGenerateResult JSON) | error(메시지)"""
import logging
from collections.abc import AsyncIterator

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.core.llm import LLMClient, LLMError, get_llm
from app.models.schemas import ChapterContext
from app.services.chapter_generator import Done, Token, generate_chapter

router = APIRouter()
log = logging.getLogger(__name__)


def sse(event: str, data: str) -> str:
    # 여러 줄 데이터는 줄마다 data: 를 붙입니다 (수신 측에서 \n으로 다시 합칩니다).
    lines = "\n".join(f"data: {line}" for line in data.split("\n"))
    return f"event: {event}\n{lines}\n\n"


@router.post("/chapter")
async def chapter(ctx: ChapterContext, llm: LLMClient = Depends(get_llm)) -> StreamingResponse:
    async def events() -> AsyncIterator[str]:
        try:
            async for item in generate_chapter(llm, ctx):
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
