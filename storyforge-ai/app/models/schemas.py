"""Spring Boot ↔ FastAPI 내부 계약. storyforge-backend의 client/AiDtos.java와 필드를 맞춥니다 (JSON은 camelCase)."""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class Schema(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ProjectInfo(Schema):
    title: str
    genre: str | None = None


class CharacterInfo(Schema):
    name: str
    description: str | None = None
    personality: str | None = None
    relationships: str | None = None
    current_state: dict[str, Any] | None = None


class BibleInfo(Schema):
    world_setting: str | None = None
    theme: str | None = None
    tone: str | None = None
    characters: list[CharacterInfo] = Field(default_factory=list)


class OutlineChapter(Schema):
    order: int
    title: str
    beats: list[str] = Field(default_factory=list)
    characters: list[str] = Field(default_factory=list)
    goals: str | None = None
    continuity_points: list[str] = Field(default_factory=list)


# ── POST /internal/generate/bible ──
class BibleGenerateRequest(Schema):
    project: ProjectInfo
    prompt: str


class GeneratedCharacter(Schema):
    name: str
    description: str
    personality: str
    relationships: str


class BibleGenerateResponse(Schema):
    world_setting: str
    theme: str
    tone: str
    characters: list[GeneratedCharacter]


# ── POST /internal/generate/outline ──
class OutlineGenerateRequest(Schema):
    project: ProjectInfo
    bible: BibleInfo
    chapter_count: int = Field(ge=1, le=50)


class OutlineGenerateResponse(Schema):
    chapters: list[OutlineChapter]


# ── POST /internal/generate/chapter (SSE) ──
class PreviousChapter(Schema):
    number: int
    title: str | None = None
    summary: str | None = None


class ChapterContext(Schema):
    """컨텍스트 패키지 (기획서 §6.3): Bible + Outline + 이전 챕터 요약 + 캐릭터 상태"""

    project: ProjectInfo
    bible: BibleInfo
    chapter_number: int
    outline_chapter: OutlineChapter
    previous_chapters: list[PreviousChapter] = Field(default_factory=list)


IssueType = Literal["setting_conflict", "character_consistency", "timeline", "plot_thread"]
Severity = Literal["low", "medium", "high"]


class ContinuityIssue(Schema):
    type: IssueType
    description: str
    severity: Severity


class ContinuityIssues(Schema):
    """LLM 검증 응답 형식"""

    issues: list[ContinuityIssue]


class ContinuityResult(Schema):
    issues: list[ContinuityIssue]
    score: int = Field(ge=0, le=100)


class ChapterGenerateResult(Schema):
    """SSE 마지막 done 이벤트 본문"""

    title: str
    content: str
    summary: str
    report: ContinuityResult
