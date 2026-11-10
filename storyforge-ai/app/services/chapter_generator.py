"""챕터 본문·요약·이어쓰기 작업. 실행 순서는 services.orchestration이 관리합니다."""
from collections.abc import AsyncGenerator

from app.core.config import get_settings
from app.core.llm import LLMClient
from app.models.schemas import ChapterContext
from app.models.orchestration import chapter_prompt_context
from app.prompts.orchestration import render
from app.services import mock_writer


async def complete_text(llm: LLMClient, before: str, after: str, tone: str | None) -> str:
    if llm.is_mock:
        return mock_writer.complete(before)
    text = await llm.text(
        system=render("system"),
        prompt=render("complete", before=before, after=after, tone=tone or ""),
        max_tokens=600,
        model=None,
    )
    return text.strip()


def stream_body(llm: LLMClient, ctx: ChapterContext) -> AsyncGenerator[str, None]:
    if llm.is_mock:
        return mock_writer.stream_text(mock_writer.chapter_text(ctx), get_settings().mock_stream_delay)
    return llm.stream(
        system=render("system"),
        prompt=render(
            "chapter",
            **chapter_prompt_context(ctx),
        ),
        model=ctx.project.model,
    )


async def summarize(llm: LLMClient, ctx: ChapterContext, content: str) -> str:
    """다음 챕터 컨텍스트로 쓰일 요약"""
    if llm.is_mock:
        return mock_writer.summary(ctx)
    text = await llm.text(
        system=render("system"), prompt=render("summary", content=content), max_tokens=2000, model=ctx.project.model
    )
    return text.strip()
