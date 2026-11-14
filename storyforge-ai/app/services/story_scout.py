"""다음 장면의 개척안.

Story Bible과 Outline, 직전 장면 안에 있는 사실만 다음 행동으로 바꿉니다.
새 인물·장소·과거 사건을 만들지 않으며, 검토 단계나 상태 전이를 대신하지 않습니다.
"""
from __future__ import annotations

import re

from app.core.llm import LLMClient
from app.models.schemas import ChapterContext, ScoutDirection, ScoutRequest, ScoutResponse
from app.prompts.orchestration import render

_STOP = {
    "그리고", "그러나", "그때", "직전", "장면", "선택", "결과", "이번", "다음", "같은", "있는", "했다", "한다",
    "바로", "순간", "이야기", "사람", "그것", "이것", "저것", "때문", "위해", "통해", "대해", "에서", "으로",
}


def carry_facts(text: str, names: list[str] | None = None) -> list[str]:
    """직전 장면의 마지막 문장과, 그 문장에 남은 물건·이름만 뽑습니다."""
    cleaned = " ".join(text.split())
    if len(cleaned) < 8:
        return []
    pieces = [part.strip() for part in re.split(r"[.!?。]\s*", cleaned) if part.strip()]
    sentence = (pieces[-1] if pieces else cleaned)[-48:]
    facts = [sentence]
    for name in names or []:
        if name and name in cleaned and name not in facts:
            facts.append(name)
    for token in re.findall(r"[가-힣]{2,}", sentence):
        if token in _STOP or any(token in fact for fact in facts):
            continue
        facts.append(token)
        if len(facts) >= 5:
            break
    return facts


def rule_directions(request: ScoutRequest) -> list[ScoutDirection]:
    """모델 없이도 직전 장면의 구체적 사실과 이번 Beat를 세 갈래로 잇습니다."""
    ending = (request.previous_ending or "").strip()
    names = [c.name for c in request.bible.characters if c.name]
    facts = carry_facts(ending, names)
    kept = " · ".join(facts) if facts else "아직 확정된 직전 장면이 없습니다"
    chapter = request.outline_chapter
    beats = list(chapter.beats) if chapter and chapter.beats else []
    first = beats[0] if beats else "이번 장에서 정한 목표를 한 걸음 진행한다"
    second = beats[1] if len(beats) > 1 else first
    who = ", ".join(names[:3]) if names else "Story Bible에 있는 인물"
    avoid = "Story Bible, Outline, 직전 장면에 없는 인물 이름·장소·과거 사건은 만들지 않는다"
    anchor = facts[0] if facts else "이야기의 첫 자리"
    return [
        ScoutDirection(
            title="바로 다음 행동",
            causal_link=f"직전 문장 「{anchor}」가 끝난 직후, 같은 물건과 같은 자리에서 다음 선택을 연다",
            next_action=first,
            avoid=avoid,
            carry=facts,
            opening=f"{anchor} 바로 다음, {first}",
        ),
        ScoutDirection(
            title="같은 인물의 대가",
            causal_link=f"{who}가 「{kept}」에서 한 선택의 대가가 이번 장면에서 드러난다",
            next_action=second,
            avoid=avoid,
            carry=facts,
            opening=f"{who}는 {anchor}의 결과를 바로 마주한다",
        ),
        ScoutDirection(
            title="열린 질문 한 걸음",
            causal_link=f"「{anchor}」에서 닫히지 않은 질문 하나만 {first}로 진행한다",
            next_action=chapter.goals if chapter and chapter.goals else first,
            avoid=avoid,
            carry=facts,
            opening=f"{anchor} 때문에 남은 질문 하나를 {first}",
        ),
    ]


def lines(context: ChapterContext) -> list[str]:
    """챕터 프롬프트에 넣을 개척 문장. 생성 결과를 바꾸어 저장하지는 않습니다."""
    ending = ""
    if context.previous_chapters:
        ending = context.previous_chapters[-1].ending_excerpt or ""
    directions = rule_directions(ScoutRequest(
        project=context.project,
        bible=context.bible,
        outline_chapter=context.outline_chapter,
        previous_ending=ending,
    ))
    return [
        f"{item.title}: 첫 문단은 「{item.opening}」로 잇습니다. 남길 사실: {', '.join(item.carry) or '없음'}. {item.causal_link} 다음 행동: {item.next_action}"
        for item in directions
    ]


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
    merged = []
    for index, item in enumerate(found.directions):
        anchor = base[min(index, len(base) - 1)]
        if not item.carry or not item.opening:
            item = item.model_copy(update={
                "carry": item.carry or anchor.carry,
                "opening": item.opening or anchor.opening,
            })
        merged.append(item)
    return ScoutResponse(directions=merged)
