"""챕터 작업의 스트림 이벤트와 필수 프롬프트 컨텍스트 계약을 한곳에서 정의합니다."""
from dataclasses import dataclass

from app.models.schemas import ChapterContext, ChapterGenerateResult


@dataclass
class Token:
    text: str


@dataclass
class Done:
    result: ChapterGenerateResult


def chapter_prompt_context(context: ChapterContext) -> dict[str, object]:
    """Bible·Outline·이전 요약·인물 상태를 자르거나 수정하지 않고 전달합니다."""
    return {
        "project": context.project,
        "bible": context.bible,
        "chapter_number": context.chapter_number,
        "previous_chapters": context.previous_chapters,
        "outline": context.outline_chapter,
        "story_outline": context.story_outline,
    }
