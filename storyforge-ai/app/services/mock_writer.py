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

# 장마다 다른 사건. 같은 문장 틀을 재사용하지 않기 위한 결정적 목록입니다.
_STAGE_BEATS = {
    "도입": [
        "주인공이 지키려던 약속의 첫 조건을 확인한다",
        "상대가 그 약속의 대가를 처음으로 말한다",
        "자리를 떠나면 약속을 잃는다는 선택을 마주한다",
    ],
    "전개": [
        "단서가 예상과 다른 사람을 가리킨다",
        "조력자가 숨겨 둔 사정을 털어놓는다",
        "되돌아가기보다 더 깊은 곳으로 들어가기로 한다",
    ],
    "위기": [
        "약속의 대가가 가까운 사람에게 돌아온다",
        "믿었던 정보가 거짓이었음이 드러난다",
        "도망치면 누군가를 버리고, 남으면 자신이 대가를 치른다",
    ],
    "절정": [
        "대립자와 같은 자리에서 약속의 원문을 맞댄다",
        "숨겨 둔 사실이 모두의 앞에서 드러난다",
        "어느 쪽을 지켜도 한쪽은 잃는 선택을 실행한다",
    ],
    "결말": [
        "선택이 남긴 결과를 당사자들이 확인한다",
        "깨진 관계와 남은 관계를 각자 말로 정리한다",
        "다음이 아니라 이번 이야기의 매듭을 짓는다",
    ],
}

_SCENES = [
    ("새벽 도매시장", "닫힌 좌판 사이에서 이름이 적힌 전표를 찾았다", "생선 비린내"),
    ("강변 창고", "녹슨 자물쇠를 열고 안에 든 편지 수신인을 확인했다", "젖은 삼나무 냄새"),
    ("비 오는 승강장", "놓친 열차 대신 플랫폼에 남은 사람과 마주 섰다", "축축한 울 코트"),
    ("폐교 음악실", "먼지 앉은 피아노 뚜껑 아래에서 오래된 악보를 꺼냈다", "마른 분필"),
    ("야간 약국", "처방전 대신 건네받은 쪽지의 주소를 읽었다", "소독약"),
    ("옥상 물탱크", "도시 불빛 아래에서 회중시계의 멈춘 시각을 맞췄다", "철 난간의 냉기"),
    ("지하 기록실", "철제 서랍에서 지워진 이름의 카드를 꺼냈다", "곰팡이 슨 종이"),
    ("해 뜨기 전 부둣가", "밧줄을 감는 인부와 눈을 맞추고 배를 기다렸다", "소금기 있는 바람"),
    ("폐쇄된 극장", "객석 한가운데 놓인 빈 의자 위에서 대사를 들었다", "낡은 벨벳"),
    ("산길 휴게소", "식은 커피를 두고 창밖에 멈춘 차를 확인했다", "젖은 아스팔트"),
]


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
        tone=project.default_tone or "3인칭 제한 시점. 차분하고 절제된 문장, 장면마다 감각 묘사를 하나씩 둔다.",
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
        beats = list(_STAGE_BEATS[stage])
        chapters.append(
            OutlineChapter(
                order=i + 1,
                title=f"{stage} — {project.title} {i + 1}",
                beats=beats,
                characters=cast,
                goals=f"{stage}: 앞 장과 다른 선택과 그 대가를 보여 준다",
                continuity_points=[beats[-1]],
            )
        )
    return chapters


def chapter_text(ctx: ChapterContext) -> str:
    oc = ctx.outline_chapter
    lead = oc.characters[0] if oc.characters else "그"
    paragraphs = [MOCK_NOTE]
    if ctx.previous_chapters:
        paragraphs.append(f"{lead}은(는) 지나온 설명을 반복하지 않고 다음 장소에 발을 들였다.")
    for index, beat in enumerate(oc.beats):
        place, action, sense = _SCENES[(ctx.chapter_number * 5 + index) % len(_SCENES)]
        other = oc.characters[-1] if len(oc.characters) > 1 else lead
        paragraphs.append(
            f"{beat}. {place}에서 {lead}은(는) {action}. {other}이(가) 곁에 있었고, "
            f"공기에 남은 것은 {sense}였다. 말은 줄이고, 그 선택이 누구에게 대가를 남기는지만 확인했다."
        )
    if oc.goals:
        paragraphs.append(f"이번 장의 방향은 이것이었다. {oc.goals}")
    return "\n\n".join(paragraphs)


def complete(before: str) -> str:
    """앞 문장을 반복하지 않는 짧은 이어쓰기."""
    seed = abs(hash(before[-80:])) % len(_SCENES)
    place, action, sense = _SCENES[seed]
    return f" {place}에서 {action}. 공기에 남은 것은 {sense}였고, 방금 한 말과는 다른 선택이 필요했다."


def summary(ctx: ChapterContext) -> str:
    oc = ctx.outline_chapter
    return f"{ctx.chapter_number}장: " + " / ".join(oc.beats)


def review_summary(ctx: ChapterContext, content: str) -> str:
    """수정본 요약. 본문이 바뀌면 요약도 바뀌도록 앞부분을 씁니다."""
    body = content.replace(MOCK_NOTE, "").strip()
    return f"{ctx.chapter_number}장(수정본): {body[:120]}"


async def stream_text(text: str, delay: float) -> AsyncIterator[str]:
    """실제 스트리밍처럼 몇 글자씩 나눠 보냅니다."""
    step = 6
    for i in range(0, len(text), step):
        yield text[i : i + step]
        await asyncio.sleep(delay)
