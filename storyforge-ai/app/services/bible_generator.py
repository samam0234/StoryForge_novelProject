"""Story Bible 초기 생성 (기획서 §6.1)"""
from app.core.llm import LLMClient
from app.models.schemas import BibleGenerateRequest, BibleGenerateResponse
from app.prompts import render
from app.services import mock_writer


async def generate_bible(llm: LLMClient, request: BibleGenerateRequest) -> BibleGenerateResponse:
    if llm.is_mock:
        return mock_writer.bible(request.project, request.prompt)
    from app.services.character_bank import CharacterBank

    bank = CharacterBank()
    assigned = bank.reserve(4)
    avoid = bank.similar_notes(request.prompt)
    result = await llm.structured(
        system=render("system"),
        prompt=render(
            "bible",
            project=request.project,
            prompt=request.prompt,
            assigned_names=assigned,
            avoid_notes=avoid,
        ),
        schema=BibleGenerateResponse,
        model=request.project.model,
    )
    allowed = {row["name"] for row in assigned}
    used = {row["name"] for row in bank.used()}
    spare = [row["name"] for row in assigned]
    for character in result.characters:
        if character.name in used or character.name not in allowed:
            replacement = next((name for name in spare if name not in {c.name for c in result.characters}), None)
            if replacement:
                character.name = replacement
    bank.commit(result.characters, request.project.title)
    return result
