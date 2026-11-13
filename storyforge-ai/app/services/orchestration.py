"""AI 작업의 공통 실행 경계. 생성·요약·Continuity를 조정하고 DB 상태는 다루지 않습니다."""
from collections.abc import AsyncGenerator
from contextlib import aclosing

from app.core.llm import LLMClient
from app.models.orchestration import Done, Token
from app.models.schemas import (
    BibleGenerateRequest, BibleGenerateResponse, ChapterContext, ChapterGenerateResult,
    ChapterReviewRequest, ChapterReviewResult, GuideChatRequest, GuideChatResponse,
    OutlineGenerateRequest, OutlineGenerateResponse, PackageContinuityRequest, PackageContinuityResult,
)
from app.services import bible_generator, chapter_generator, guide_assistant, mock_writer
from app.services import outline_generator, package_continuity
from app.services.continuity_checker import check_continuity
from app.services.repetition_guard import RepetitionGuard, drop_repeated_sentences
from app.services.chapter_analysis import analyze


class AiTaskOrchestrator:
    def __init__(self, llm: LLMClient) -> None:
        self.llm = llm

    async def generate_bible(self, request: BibleGenerateRequest) -> BibleGenerateResponse:
        return await bible_generator.generate_bible(self.llm, request)

    async def generate_outline(self, request: OutlineGenerateRequest) -> OutlineGenerateResponse:
        return await outline_generator.generate_outline(self.llm, request)

    async def generate_chapter(self, context: ChapterContext) -> AsyncGenerator[Token | Done, None]:
        parts: list[str] = []
        guard = RepetitionGuard()
        async with aclosing(chapter_generator.stream_body(self.llm, context)) as stream:
            async for chunk in stream:
                guard.feed(chunk)
                parts.append(chunk)
                yield Token(chunk)
        content = drop_repeated_sentences("".join(parts).strip())
        if self.llm.is_mock:
            summary = await chapter_generator.summarize(self.llm, context, content)
            report = await check_continuity(self.llm, context, content)
        else:
            analysis = await analyze(self.llm, context, content)
            summary, report = analysis.summary, analysis.report
        yield Done(ChapterGenerateResult(title=context.outline_chapter.title, content=content,
                                        summary=summary, report=report))

    async def review_chapter(self, request: ChapterReviewRequest) -> ChapterReviewResult:
        if not self.llm.is_mock:
            return await analyze(self.llm, request.context, request.content)
        summary = mock_writer.review_summary(request.context, request.content)
        report = await check_continuity(self.llm, request.context, request.content)
        return ChapterReviewResult(summary=summary, report=report)

    async def complete(self, before: str, after: str, tone: str | None) -> str:
        return await chapter_generator.complete_text(self.llm, before, after, tone)

    async def check_package(self, request: PackageContinuityRequest) -> PackageContinuityResult:
        return await package_continuity.check_package(self.llm, request)

    async def guide(self, request: GuideChatRequest) -> GuideChatResponse:
        return await guide_assistant.answer(self.llm, request)
