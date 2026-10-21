"""LLM_PROVIDER=mock일 때 쓰는 결정적(deterministic) 생성기.

API 키 없이 파이프라인 전체(Bible → Outline → 챕터 스트리밍 → Continuity)를 확인하기 위한 용도입니다.
생성물에는 mock 표시를 남겨 실제 AI 결과와 헷갈리지 않게 합니다.
"""
from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator

from app.models.schemas import (
    BibleGenerateResponse,
    BibleInfo,
    ChapterContext,
    GeneratedCharacter,
    OutlineChapter,
    ProjectInfo,
)

MOCK_NOTE = "※ 개발용 mock 생성기 출력입니다 (LLM_PROVIDER=mock)."

_STAGES = ["도입", "전개", "위기", "절정", "결말"]


def bible(project: ProjectInfo, prompt: str) -> BibleGenerateResponse:
    genre = project.genre or "드라마"
    return BibleGenerateResponse(
        world_setting=(
            f"{MOCK_NOTE}\n\n"
            f"「{project.title}」의 무대는 {genre} 장르의 규칙을 따르는 세계다.\n"
            f"작가의 아이디어: {prompt.strip()}\n\n"
            "이 세계에서는 한번 맺은 약속을 어기면 대가를 치른다는 규칙이 모든 사건의 바탕에 깔려 있다."
        ),
        theme="잃어버린 것을 되찾으려는 사람이 끝내 무엇을 지키기로 선택하는가.",
        tone="3인칭 제한 시점. 차분하고 절제된 문장, 장면마다 감각 묘사를 하나씩 둔다.",
        characters=[
            GeneratedCharacter(
                name="한서윤",
                description="스물여덟 살. 왼손에 오래된 화상 자국이 있다.",
                personality="신중하고 고집이 세다. 위기 앞에서 오히려 침착해진다.",
                relationships="강도현과는 오래된 동료, 윤재하에게는 갚지 못한 빚이 있다.",
            ),
            GeneratedCharacter(
                name="강도현",
                description="서른한 살. 늘 낡은 회중시계를 지니고 다닌다.",
                personality="농담으로 불안을 감추는 낙천가. 약속은 반드시 지킨다.",
                relationships="한서윤의 조력자. 윤재하와는 과거에 같은 스승 밑에서 배웠다.",
            ),
            GeneratedCharacter(
                name="윤재하",
                description="나이를 짐작하기 어렵다. 목소리가 낮고 조용하다.",
                personality="목적을 위해 수단을 가리지 않지만 자신만의 원칙이 있다.",
                relationships="한서윤과 강도현 모두와 얽힌 과거가 있는 대립자.",
            ),
        ],
    )


def outline(project: ProjectInfo, bible_info: BibleInfo, count: int) -> list[OutlineChapter]:
    names = [c.name for c in bible_info.characters] or ["주인공"]
    chapters = []
    for i in range(count):
        stage = _STAGES[min(i * len(_STAGES) // count, len(_STAGES) - 1)]
        cast = [names[0]] + ([names[(i % (len(names) - 1)) + 1]] if len(names) > 1 else [])
        chapters.append(
            OutlineChapter(
                order=i + 1,
                title=f"{stage} — {i + 1}번째 약속",
                beats=[
                    f"{cast[0]}이(가) {i + 1}번째 단서를 발견한다",
                    f"{cast[-1]}와(과)의 대화에서 숨겨진 사정이 드러난다",
                    "선택의 순간이 다가온다",
                ],
                characters=cast,
                goals=f"{stage}: 긴장을 한 단계 끌어올린다",
                continuity_points=[f"{i + 1}번째 단서의 행방"],
            )
        )
    return chapters


def chapter_text(ctx: ChapterContext) -> str:
    oc = ctx.outline_chapter
    prev = ctx.previous_chapters[-1].summary if ctx.previous_chapters else None
    lead = oc.characters[0] if oc.characters else "그"
    paragraphs = [MOCK_NOTE]
    if prev:
        paragraphs.append(f"지난 일이 아직 끝나지 않았다. {prev}")
    for beat in oc.beats:
        paragraphs.append(
            f"{beat}. {lead}은(는) 숨을 고르며 주변을 살폈다. 바람이 멎은 골목에는 낡은 등불만 흔들렸고, "
            "그 불빛 아래에서 오래 묻어 두었던 기억이 천천히 떠올랐다. 지금 물러서면 다시는 같은 기회가 오지 않으리라는 것을 알았다."
        )
    if oc.goals:
        paragraphs.append(f"({oc.goals})")
    return "\n\n".join(paragraphs)


def summary(ctx: ChapterContext) -> str:
    oc = ctx.outline_chapter
    return f"{ctx.chapter_number}장: " + " / ".join(oc.beats)


async def stream_text(text: str, delay: float) -> AsyncIterator[str]:
    """실제 스트리밍처럼 몇 글자씩 나눠 보냅니다."""
    step = 6
    for i in range(0, len(text), step):
        yield text[i : i + step]
        await asyncio.sleep(delay)
