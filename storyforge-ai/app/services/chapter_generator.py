"""챕터 생성 (기획서 §6.3, §7.2 4단계).

컨텍스트(Bible + Outline + 이전 챕터 요약 + 캐릭터 상태)를 프롬프트에 주입해 스트리밍 생성하고,
생성이 끝나면 요약과 Continuity 검증을 실행합니다.
"""
from collections.abc import AsyncIterator
from dataclasses import dataclass

from app.core.config import get_settings
from app.core.llm import LLMClient
from app.models.schemas import ChapterContext, ChapterGenerateResult, ChapterReviewRequest, ChapterReviewResult
from app.prompts import render
from app.services import mock_writer
from app.services.continuity_checker import check_continuity


@dataclass
class Token:
    text: str


@dataclass
class Done:
    result: ChapterGenerateResult


async def generate_chapter(llm: LLMClient, ctx: ChapterContext) -> AsyncIterator[Token | Done]:
    parts: list[str] = []
    async for chunk in _stream_body(llm, ctx):
        parts.append(chunk)
        yield Token(chunk)
    content = "".join(parts).strip()

    summary = await _summarize(llm, ctx, content)
    report = await check_continuity(llm, ctx, content)
    yield Done(
        ChapterGenerateResult(
            title=ctx.outline_chapter.title,
            content=content,
            summary=summary,
            report=report,
        )
    )


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


async def review_chapter(llm: LLMClient, request: ChapterReviewRequest) -> ChapterReviewResult:
    """사람이 고친 본문을 기준으로 요약과 Continuity 검증을 다시 합니다."""
    ctx = request.context
    if llm.is_mock:
        summary = mock_writer.review_summary(ctx, request.content)
    else:
        summary = await _summarize(llm, ctx, request.content)
    report = await check_continuity(llm, ctx, request.content)
    return ChapterReviewResult(summary=summary, report=report)


def _stream_body(llm: LLMClient, ctx: ChapterContext) -> AsyncIterator[str]:
    if llm.is_mock:
        return mock_writer.stream_text(mock_writer.chapter_text(ctx), get_settings().mock_stream_delay)
    return llm.stream(
        system=render("system"),
        prompt=render(
            "chapter",
            project=ctx.project,
            bible=ctx.bible,
            chapter_number=ctx.chapter_number,
            previous_chapters=ctx.previous_chapters,
            outline=ctx.outline_chapter,
        ),
        model=ctx.project.model,
    )


async def _summarize(llm: LLMClient, ctx: ChapterContext, content: str) -> str:
    """다음 챕터 컨텍스트로 쓰일 요약"""
    if llm.is_mock:
        return mock_writer.summary(ctx)
    text = await llm.text(
        system=render("system"), prompt=render("summary", content=content), max_tokens=2000, model=ctx.project.model
    )
    return text.strip()
