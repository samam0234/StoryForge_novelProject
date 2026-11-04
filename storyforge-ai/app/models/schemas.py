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
    # 프로젝트 설정 (기획서 §9.6-5). 비어 있으면 서비스 기본값을 씁니다.
    model: str | None = None
    default_tone: str | None = None


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


# ── POST /internal/generate/review ──
class ChapterReviewRequest(Schema):
    """사람이 고친 원고의 재요약·재검증"""

    context: ChapterContext
    title: str | None = None
    content: str


class ChapterReviewResult(Schema):
    summary: str
    report: ContinuityResult


# ── POST /internal/package/continuity ──
class PackageWorkInfo(Schema):
    title: str
    role: str | None = None
    bible: BibleInfo
    chapters: list[PreviousChapter] = Field(default_factory=list)


class PackageLinkInfo(Schema):
    from_title: str
    to_title: str
    type: str
    label: str | None = None


class PackageContinuityRequest(Schema):
    title: str
    world_note: str | None = None
    works: list[PackageWorkInfo]
    links: list[PackageLinkInfo] = Field(default_factory=list)


class PackageIssue(Schema):
    type: IssueType
    description: str
    severity: Severity
    work_titles: list[str] = Field(default_factory=list)


class PackageIssues(Schema):
    """LLM 검증 응답 형식"""

    issues: list[PackageIssue]


class PackageContinuityResult(Schema):
    issues: list[PackageIssue]
    score: int = Field(ge=0, le=100)


# ── POST /internal/guide/chat ──
class GuideContext(Schema):
    """가이드챗이 참고할 사용자 상황. Spring Boot가 채웁니다."""

    display_name: str
    project_count: int = 0
    published_works: int = 0
    project_id: int | None = None
    project_title: str | None = None
    project_status: str | None = None
    next_chapter: int | None = None


class GuideChatRequest(Schema):
    message: str = Field(max_length=2000)
    context: GuideContext


class GuideAction(Schema):
    """kind: route(앱 안 이동), collab(외부 연결, 주소는 Spring Boot가 채움)"""

    label: str
    kind: Literal["route", "collab"]
    target: str


class GuideChatResponse(Schema):
    reply: str
    actions: list[GuideAction] = Field(default_factory=list)
