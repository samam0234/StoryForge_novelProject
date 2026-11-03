"""AI 가이드챗. 명령어는 정해진 안내를 돌려주고, 자유 질문만 LLM에 묻습니다.

이동 버튼(actions)은 항상 이 파일의 표에서 만듭니다. 모델 응답에 든 주소나 경로는 버튼이 되지 않습니다.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from app.core.llm import LLMClient
from app.models.schemas import GuideAction, GuideChatRequest, GuideChatResponse, GuideContext
from app.prompts import render

_STATUS_NEXT = {
    "BIBLE_DRAFT": ("Story Bible을 작성하고 확정할 차례입니다.", "bible"),
    "BIBLE_CONFIRMED": ("Outline을 만들 차례입니다.", "outline"),
    "OUTLINE_DRAFT": ("Outline을 다듬고 확정할 차례입니다.", "outline"),
    "OUTLINE_CONFIRMED": ("첫 챕터를 생성할 차례입니다.", "chapters"),
    "CHAPTER_GENERATING": ("챕터를 생성하는 중입니다.", "chapters"),
    "CHAPTER_REVIEW": ("챕터를 검토하고 승인할 차례입니다.", "chapters"),
    "COMPLETED": ("모든 챕터를 승인했습니다. 게시하거나 내보낼 수 있습니다.", "publish"),
}


@dataclass(frozen=True)
class Topic:
    reply: str
    actions: tuple[GuideAction, ...] = field(default=())
    keywords: tuple[str, ...] = field(default=())


def _route(label: str, target: str) -> GuideAction:
    return GuideAction(label=label, kind="route", target=target)


TOPICS: dict[str, Topic] = {
    "start": Topic(
        "장편은 Story Bible → Outline → 챕터 순서로 만듭니다.\n"
        "1. 프로젝트를 만들고 아이디어로 Story Bible을 생성합니다.\n"
        "2. Bible을 확정하면 Outline을 만들 수 있습니다.\n"
        "3. 챕터를 하나씩 생성하고, 검토한 뒤 승인합니다.",
        (_route("창작하기", "/create"),),
        ("시작", "처음", "만들"),
    ),
    "bible": Topic(
        "Story Bible은 세계관, 테마, 톤, 인물을 정리한 설정집입니다. 모든 챕터 생성에 컨텍스트로 들어갑니다.\n"
        "AI로 초안을 만들고 직접 고친 뒤 확정하세요. 확정하면 그 프로젝트에서는 더 고칠 수 없습니다.",
        (_route("내 프로젝트", "/"),),
        ("바이블", "bible", "세계관", "설정", "인물", "캐릭터"),
    ),
    "outline": Topic(
        "Outline은 챕터별 제목, 사건(Beats), 등장인물, 목표, 연속성 포인트입니다.\n"
        "끌어서 순서를 바꿀 수 있습니다. 단편은 챕터 수를 1~3개로 잡으면 충분합니다.",
        (_route("내 프로젝트", "/"),),
        ("아웃라인", "outline", "플롯", "줄거리", "목차"),
    ),
    "chapter": Topic(
        "챕터는 1장부터 순서대로 생성합니다. 앞 장을 승인해야 다음 장을 만들 수 있습니다.\n"
        "생성이 끝나면 Continuity 리포트를 확인하고, 고친 뒤 승인하세요.",
        (_route("내 프로젝트", "/"),),
        ("챕터", "chapter", "생성", "장 "),
    ),
    "continuity": Topic(
        "Continuity 리포트는 설정 충돌, 인물 일관성, 타임라인, 복선을 검사한 결과입니다.\n"
        "심각도 high 항목은 승인 전에 꼭 확인하세요. 재생성하거나 직접 고칠 수 있습니다.",
        (),
        ("연속성", "continuity", "검증", "충돌", "리포트"),
    ),
    "publish": Topic(
        "프로젝트의 '게시' 탭에서 승인된 챕터를 작품으로 올립니다. 롱품 또는 숏품을 고르고, 공개 범위와 Clone 정책을 정하세요.\n"
        "검토 중인 초안은 게시되지 않습니다. 게시 후에는 '내 작품 관리'에서 본문과 정보를 고칠 수 있습니다.",
        (_route("내 작품 관리", "/me/works"),),
        ("게시", "올리", "출간", "공개", "발행"),
    ),
    "clone": Topic(
        "Clone은 작품의 창작 자료를 내 프로젝트로 가져와 이어 쓰는 기능입니다. 범위는 셋입니다.\n"
        "- 세계관: Story Bible만. 내 이야기를 새로 씁니다.\n"
        "- 플롯: Bible과 Outline. 줄거리를 고쳐 다시 씁니다.\n"
        "- 전체 원고: 게시된 원고까지. 앞 장부터 다시 승인합니다.\n"
        "원본과의 차이는 프로젝트의 '원본 비교' 탭에서 볼 수 있습니다.",
        (_route("롱품 둘러보기", "/board/long"),),
        ("clone", "클론", "복제", "포크", "fork", "이어 쓰"),
    ),
    "share": Topic(
        "공유는 읽을 링크를 전달하는 기능입니다. 어디에 공유했는지 고르면 전용 링크가 만들어지고, "
        "작품의 공유 그래프에 누가 어디에 공유했는지와 방문 수가 나옵니다. 입력한 내용은 공개됩니다.",
        (),
        ("공유", "share", "링크"),
    ),
    "package": Topic(
        "패키지포지는 내가 게시한 작품을 묶어 하나의 세계로 만드는 기능입니다.\n"
        "유형: 유니버스, 옴니버스, 앤솔러지, 사가, 크로스오버, 스핀오프, 커스텀.\n"
        "독서 순서, 작품 연결 지도, 통합 인물 사전, 통합 내보내기를 제공합니다. 다른 작가의 작품은 그 작가가 승인하면 들어옵니다.",
        (_route("패키지포지", "/package-forge"), _route("패키지 둘러보기", "/board/package")),
        ("패키지", "package", "유니버스", "옴니버스", "통합", "앤솔러지", "크로스오버", "시리즈"),
    ),
    "export": Topic(
        "내보내기 형식은 Markdown, TXT, Word(DOCX), PDF, HTML, EPUB, ODT, RTF입니다. 승인된 챕터만 들어갑니다.\n"
        "프로젝트 상단 또는 작품 화면의 내보내기 버튼을 쓰세요.",
        (),
        ("내보내", "export", "다운로드", "pdf", "epub", "docx", "파일"),
    ),
    "board": Topic(
        "게시판은 롱품(장편), 숏품(단편), 패키지포지(통합본), 창작하기(프로젝트 시작과 라운지), 이슈(인기작과 축하)로 나뉩니다.",
        (
            _route("롱품", "/board/long"),
            _route("숏품", "/board/short"),
            _route("패키지포지", "/board/package"),
            _route("창작하기", "/create"),
            _route("이슈", "/issues"),
        ),
        ("게시판", "커뮤니티", "롱품", "숏품", "라운지"),
    ),
    "issue": Topic(
        "이슈 게시판은 최근 7일 반응이 좋은 작품과 달성 축하(예: 하루 만에 추천 100개)를 보여 줍니다.",
        (_route("이슈 보기", "/issues"),),
        ("이슈", "인기", "축하", "달성"),
    ),
    "profile": Topic(
        "프로필에는 작품 목록, 활동 잔디(활동이 많은 날일수록 진함), 활동 내역, 업적이 나옵니다.\n"
        "이름, 소개, 비밀번호는 계정 설정에서 바꿉니다.",
        (_route("계정 설정", "/me/settings"),),
        ("프로필", "잔디", "업적", "배지", "계정", "비밀번호", "활동"),
    ),
    "graph": Topic(
        "작업 그래프는 날짜별 작업을 수동과 에이전트로 나눠 보여 줍니다. 막대를 누르면 그날 한 작업이 표로 나오고, "
        "작업 기록표의 점을 누르면 그 기록 문단이 열립니다.\n"
        "프로필의 활동 잔디도 날짜를 누르면 그날 기록 문단이 나옵니다. 본문은 공개되지 않고 '3장 직접 수정'처럼 작업 내용만 보입니다.",
        (),
        ("그래프", "수동", "에이전트", "비율", "기록", "잔디"),
    ),
    "collab": Topic(
        "함께 쓰려면 창작하기에서 초대를 수락하거나, 프로젝트 작업 기록에서 이메일로 초대합니다. "
        "수락한 사람만 원고를 보고, 작업 커밋에 고친 구간을 글로 남기면 점을 눌렀을 때 그 문장이 표시됩니다.\n"
        "Codicomperchat 주소가 있으면 새 창으로도 이동할 수 있습니다.",
        (
            _route("창작하기", "/create"),
            GuideAction(label="Codicomperchat으로 이동", kind="collab", target="codicomperchat"),
        ),
        ("codicomper", "코디컴퍼", "콜라보", "협업", "mcp", "커넥터", "초대", "커밋"),
    ),
}

COMMANDS: dict[str, str] = {
    "시작": "start", "start": "start",
    "바이블": "bible", "세계관": "bible", "bible": "bible",
    "아웃라인": "outline", "플롯": "outline", "outline": "outline",
    "챕터": "chapter", "chapter": "chapter",
    "검증": "continuity", "연속성": "continuity", "continuity": "continuity",
    "게시": "publish", "publish": "publish",
    "클론": "clone", "clone": "clone",
    "공유": "share", "share": "share",
    "패키지": "package", "패키지포지": "package", "package": "package",
    "내보내기": "export", "export": "export",
    "게시판": "board", "board": "board",
    "이슈": "issue", "issue": "issue",
    "프로필": "profile", "계정": "profile", "profile": "profile",
    "그래프": "graph", "graph": "graph",
    "콜라보": "collab", "collab": "collab", "codicomper": "collab", "codicomperchat": "collab",
}

HELP = (
    "쓸 수 있는 명령어입니다.\n"
    "/상태 내 프로젝트의 다음 단계\n"
    "/시작 /바이블 /아웃라인 /챕터 /검증 창작 절차\n"
    "/게시 /클론 /공유 /패키지 /내보내기 게시와 배포\n"
    "/게시판 /이슈 /프로필 /그래프 커뮤니티\n"
    "/콜라보 Codicomperchat으로 이동\n"
    "명령어 없이 물어봐도 됩니다."
)


def _status(ctx: GuideContext) -> GuideChatResponse:
    if ctx.project_id is None:
        return GuideChatResponse(
            reply=f"{ctx.display_name}님, 아직 프로젝트가 없습니다. 창작하기에서 시작해 보세요.",
            actions=[_route("창작하기", "/create")],
        )
    message, tab = _STATUS_NEXT.get(ctx.project_status or "", ("프로젝트를 확인해 보세요.", "bible"))
    if ctx.project_status in ("OUTLINE_CONFIRMED", "CHAPTER_REVIEW") and ctx.next_chapter:
        message += f" 지금 다룰 챕터는 {ctx.next_chapter}장입니다."
    reply = (
        f"최근 프로젝트 「{ctx.project_title}」: {message}\n"
        f"프로젝트 {ctx.project_count}개, 공개 작품 {ctx.published_works}개가 있습니다."
    )
    return GuideChatResponse(reply=reply, actions=[_route("이어서 작업", f"/projects/{ctx.project_id}/{tab}")])


def _topic(key: str) -> GuideChatResponse:
    t = TOPICS[key]
    return GuideChatResponse(reply=t.reply, actions=list(t.actions))


def match_topic(message: str) -> str | None:
    """자유 질문에서 가장 많이 맞는 주제를 찾습니다. 없으면 None입니다."""
    text = message.lower()
    best, best_score = None, 0
    for key, t in TOPICS.items():
        # 긴 키워드에 더 큰 점수를 줍니다. "게시판"이 "게시"(게시 안내)로 빠지지 않게 합니다.
        score = sum(len(k) for k in t.keywords if k in text)
        if score > best_score:
            best, best_score = key, score
    return best


async def answer(llm: LLMClient, request: GuideChatRequest) -> GuideChatResponse:
    message = request.message.strip()
    if message.startswith("/"):
        command = message[1:].split()[0].lower() if len(message) > 1 else ""
        if command in ("도움말", "help", ""):
            return GuideChatResponse(reply=HELP, actions=[])
        if command in ("상태", "status"):
            return _status(request.context)
        key = COMMANDS.get(command)
        if key is None:
            return GuideChatResponse(reply=f"/{command} 명령어는 없습니다.\n\n{HELP}", actions=[])
        return _topic(key)

    key = match_topic(message)
    actions = list(TOPICS[key].actions) if key else []
    if llm.is_mock:
        if key:
            return GuideChatResponse(reply=TOPICS[key].reply, actions=actions)
        return GuideChatResponse(reply=f"정확히 맞는 안내를 찾지 못했습니다.\n\n{HELP}", actions=[])

    reply = await llm.text(
        system=render("guide"),
        prompt=render("guide_question", context=request.context, reference=TOPICS[key].reply if key else None, question=message),
        max_tokens=1500,
    )
    return GuideChatResponse(reply=reply.strip(), actions=actions)
