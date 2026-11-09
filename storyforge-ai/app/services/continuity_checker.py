"""Continuity 검증 (기획서 §6.3): 설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드.

반복 검사는 새 유형을 만들지 않고 plot_thread로 남깁니다. 상태 머신과 이슈 종류는 그대로입니다.

규칙 기반 검사는 항상 실행하고, 실제 LLM이 연결되어 있으면 LLM 검증 결과를 더합니다.
점수는 이슈 심각도로 계산하며, 결과를 임의로 통과 처리하지 않습니다.
"""
import re
from collections import Counter

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
    issues.extend(_repetition_issues(ctx, content))
    return issues


def _repetition_issues(ctx: ChapterContext, content: str) -> list[ContinuityIssue]:
    """이전 장 요약을 그대로 붙이거나, 같은 문장 틀을 여러 번 쓰면 흐름이 멈춘 것으로 봅니다."""
    found: list[ContinuityIssue] = []
    for prev in ctx.previous_chapters:
        summary = (prev.summary or "").strip()
        if len(summary) >= 24 and summary in content:
            found.append(
                ContinuityIssue(
                    type="plot_thread",
                    description=f"{prev.number}장 요약이 본문에 그대로 들어가 같은 내용을 다시 서술합니다.",
                    severity="medium",
                )
            )
    openings = [part.strip()[:18] for part in re.split(r"[.!?。]\s*", content) if len(part.strip()) >= 18]
    repeated = [text for text, count in Counter(openings).items() if count >= 3]
    if repeated:
        found.append(
            ContinuityIssue(
                type="plot_thread",
                description=f"같은 문장 시작이 {repeated[0]!r} 형태로 반복되어 장면이 진행되지 않습니다.",
                severity="medium",
            )
        )
    return found


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
            model=ctx.project.model,
        )
        issues.extend(found.issues)
    return ContinuityResult(issues=issues, score=score(issues))
