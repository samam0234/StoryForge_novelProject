"""Continuity 검증 (기획서 §6.3): 설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드.

규칙 기반 검사는 항상 실행하고, 실제 LLM이 연결되어 있으면 LLM 검증 결과를 더합니다.
점수는 이슈 심각도로 계산하며, 결과를 임의로 통과 처리하지 않습니다.
"""
from app.core.llm import LLMClient
from app.models.schemas import (
    ChapterContext,
    ContinuityIssue,
    ContinuityIssues,
    ContinuityResult,
)
from app.prompts import render

_PENALTY = {"high": 30, "medium": 12, "low": 4}
_MIN_LENGTH = 300


def rule_checks(ctx: ChapterContext, content: str) -> list[ContinuityIssue]:
    issues: list[ContinuityIssue] = []
    for name in ctx.outline_chapter.characters:
        if name and name not in content:
            issues.append(
                ContinuityIssue(
                    type="plot_thread",
                    description=f"Outline에 등장하기로 한 '{name}'이(가) 본문에 나오지 않습니다.",
                    severity="medium",
                )
            )
    known = {c.name for c in ctx.bible.characters}
    for name in ctx.outline_chapter.characters:
        if name and known and name not in known:
            issues.append(
                ContinuityIssue(
                    type="character_consistency",
                    description=f"'{name}'은(는) Story Bible에 없는 캐릭터입니다.",
                    severity="low",
                )
            )
    if len(content.strip()) < _MIN_LENGTH:
        issues.append(
            ContinuityIssue(
                type="plot_thread",
                description=f"본문이 {len(content.strip())}자로 너무 짧아 Beats를 충분히 다루지 못했을 수 있습니다.",
                severity="low",
            )
        )
    return issues


def score(issues: list[ContinuityIssue]) -> int:
    return max(0, 100 - sum(_PENALTY[i.severity] for i in issues))


async def check_continuity(llm: LLMClient, ctx: ChapterContext, content: str) -> ContinuityResult:
    issues = rule_checks(ctx, content)
    if not llm.is_mock:
        found = await llm.structured(
            system=render("system"),
            prompt=render(
                "continuity",
                bible=ctx.bible,
                previous_chapters=ctx.previous_chapters,
                outline=ctx.outline_chapter,
                content=content,
            ),
            schema=ContinuityIssues,
        )
        issues.extend(found.issues)
    return ContinuityResult(issues=issues, score=score(issues))
