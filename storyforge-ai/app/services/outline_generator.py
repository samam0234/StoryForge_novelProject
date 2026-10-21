"""Outline 생성 (기획서 §6.2)"""
from app.core.llm import LLMClient, LLMError
from app.models.schemas import OutlineGenerateRequest, OutlineGenerateResponse
from app.prompts import render
from app.services import mock_writer


async def generate_outline(llm: LLMClient, request: OutlineGenerateRequest) -> OutlineGenerateResponse:
    if llm.is_mock:
        chapters = mock_writer.outline(request.project, request.bible, request.chapter_count)
        return OutlineGenerateResponse(chapters=chapters)

    result = await llm.structured(
        system=render("system"),
        prompt=render(
            "outline",
            project=request.project,
            bible=request.bible,
            chapter_count=request.chapter_count,
        ),
        schema=OutlineGenerateResponse,
    )
    if not result.chapters:
        raise LLMError("Outline 챕터가 비어 있습니다.")
    # 순서는 응답 목록 순서로 다시 매깁니다.
    for i, c in enumerate(result.chapters, start=1):
        c.order = i
    return result
