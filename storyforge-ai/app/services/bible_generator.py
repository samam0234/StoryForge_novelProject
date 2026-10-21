"""Story Bible 초기 생성 (기획서 §6.1)"""
from app.core.llm import LLMClient
from app.models.schemas import BibleGenerateRequest, BibleGenerateResponse
from app.prompts import render
from app.services import mock_writer


async def generate_bible(llm: LLMClient, request: BibleGenerateRequest) -> BibleGenerateResponse:
    if llm.is_mock:
        return mock_writer.bible(request.project, request.prompt)
    return await llm.structured(
        system=render("system"),
        prompt=render("bible", project=request.project, prompt=request.prompt),
        schema=BibleGenerateResponse,
    )
