"""다음 장면의 개척안.

Story Bible과 Outline, 직전 장면 안에 있는 사실만 다음 행동으로 바꿉니다.
새 인물·장소·과거 사건을 만들지 않으며, 검토 단계나 상태 전이를 대신하지 않습니다.
"""
from __future__ import annotations

from app.core.llm import LLMClient
from app.models.schemas import ChapterContext, ScoutDirection, ScoutRequest, ScoutResponse
from app.prompts.orchestration import render


def rule_directions(request: ScoutRequest) -> list[ScoutDirection]:
    """모델 없이도 직전 장면과 이번 Beat에서 세 갈래를 만듭니다."""
    ending = (request.previous_ending or "").strip()
    tail = ending[-48:] if ending else "아직 아무 장면도 시작되지 않은 첫 자리"
    chapter = request.outline_chapter
    beats = list(chapter.beats) if chapter and chapter.beats else []
    first = beats[0] if beats else "이번 장에서 정한 목표를 한 걸음 진행한다"
    second = beats[1] if len(beats) > 1 else first
    names = [c.name for c in request.bible.characters if c.name][:3]
    who = ", ".join(names) if names else "Story Bible에 있는 인물"
    avoid = "Story Bible, Outline, 직전 장면에 없는 인물 이름·장소·과거 사건은 만들지 않는다"
    link = f"직전 장면의 마지막 행동 「{tail}」 직후, 같은 자리에서 다음 선택을 연다"
    return [
        ScoutDirection(title="바로 다음 행동", causal_link=link, next_action=first, avoid=avoid),
        ScoutDirection(
            title="같은 인물의 대가",
            causal_link=f"{who}의 직전 선택이 이번 장면의 대가를 만든다",
            next_action=second,
            avoid=avoid,
        ),
        ScoutDirection(
            title="열린 질문 한 걸음",
            causal_link="직전 장면에서 닫히지 않은 질문 하나만 이번 행동으로 진행한다",
            next_action=chapter.goals if chapter and chapter.goals else first,
            avoid=avoid,
        ),
    ]


def lines(context: ChapterContext) -> list[str]:
    """챕터 프롬프트에 넣을 개척 문장. 생성 결과를 바꾸어 저장하지는 않습니다."""
    ending = ""
    summary = ""
    if context.previous_chapters:
        last = context.previous_chapters[-1]
        ending = last.ending_excerpt or ""
        summary = last.summary or ""
    directions = rule_directions(ScoutRequest(
        project=context.project,
        bible=context.bible,
        outline_chapter=context.outline_chapter,
        previous_ending=ending,
        previous_summary=summary,
    ))
    return [f"{item.title}: {item.causal_link} 다음 행동: {item.next_action}" for item in directions]


async def scout(llm: LLMClient, request: ScoutRequest) -> ScoutResponse:
    """규칙 개척안을 기본으로 두고, 실모델일 때만 같은 제약 안의 문장을 보강합니다."""
    base = rule_directions(request)
    if llm.is_mock:
        return ScoutResponse(directions=base)
    try:
        found = await llm.structured(
            system=render("system"),
            prompt=render(
                "scout",
                project=request.project,
                bible=request.bible,
                outline=request.outline_chapter,
                previous_ending=request.previous_ending or "",
                previous_summary=request.previous_summary or "",
                anchors=base,
            ),
            schema=ScoutResponse,
            model=request.project.model,
        )
    except Exception:
        return ScoutResponse(directions=base)
    if len(found.directions) < 1:
        return ScoutResponse(directions=base)
    return found
