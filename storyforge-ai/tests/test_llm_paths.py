"""실제 API를 호출하지 않고 LLM 경로를 검증합니다 (실제 Claude 연동 검증은 보류 중).

가짜 LLM으로 프롬프트에 필수 컨텍스트가 들어가는지(정본: Bible + Outline + 이전 챕터 요약),
오류가 어떻게 전달되는지 확인합니다.
"""
from collections.abc import AsyncGenerator

import pytest
from fastapi.testclient import TestClient

from app.core.llm import LLMError, _check_stop
from app.core.orchestration import get_llm
from app.main import app
from app.models.schemas import (
    BibleGenerateResponse,
    ChapterAnalysis,
    ContinuityIssue,
    ContinuityIssues,
    GeneratedCharacter,
    OutlineChapter,
    OutlineGenerateResponse,
)
from app.prompts.orchestration import render

PROJECT = {"title": "기억 상점", "genre": "판타지"}
BIBLE = {
    "worldSetting": "약속을 어기면 대가를 치르는 세계",
    "theme": "선택",
    "tone": "절제된 문장",
    "characters": [{"name": "한서윤", "personality": "신중함", "currentState": {"부상": "왼손"}}],
}
CONTEXT = {
    "project": PROJECT,
    "bible": BIBLE,
    "chapterNumber": 2,
    "outlineChapter": {
        "order": 2,
        "title": "두 번째 약속",
        "beats": ["단서를 발견한다"],
        "characters": ["한서윤"],
        "goals": "긴장 상승",
        "continuityPoints": ["단서의 행방"],
    },
    "previousChapters": [{"number": 1, "title": "시작", "summary": "서윤이 상점을 찾았다"}],
}


class FakeLLM:
    is_mock = False

    def __init__(self, *, stream_error: Exception | None = None, issues: list[ContinuityIssue] | None = None):
        self.prompts: list[str] = []
        self.models: list[str | None] = []
        self.stream_error = stream_error
        self.issues = issues or []

    async def structured(self, *, system, prompt, schema, model=None):
        self.models.append(model)
        self.prompts.append(prompt)
        if schema is ChapterAnalysis:
            return ChapterAnalysis(summary="요약입니다", issues=self.issues)
        if schema is ContinuityIssues:
            return ContinuityIssues(issues=self.issues)
        if schema is BibleGenerateResponse:
            character = GeneratedCharacter(name="가", description="d", personality="p", relationships="r")
            return BibleGenerateResponse(world_setting="w", theme="t", tone="o", characters=[character])
        if schema is OutlineGenerateResponse:
            chapter = OutlineChapter(order=9, title="c")
            return OutlineGenerateResponse(chapters=[chapter, chapter.model_copy(update={"order": 7})])
        raise AssertionError(schema)

    async def text(self, *, system, prompt, max_tokens=4000, model=None):
        self.models.append(model)
        self.prompts.append(prompt)
        return " 요약입니다 "

    async def stream(self, *, system, prompt, model=None) -> AsyncGenerator[str, None]:
        self.models.append(model)
        self.prompts.append(prompt)
        yield "본문 "
        if self.stream_error:
            raise self.stream_error
        yield "한서윤이 걸었다."


@pytest.fixture
def use_llm():
    def _use(llm):
        app.dependency_overrides[get_llm] = lambda: llm
        return TestClient(app)

    yield _use
    app.dependency_overrides.clear()


def _events(body: str) -> list[tuple[str, str]]:
    out = []
    for block in filter(None, body.split("\n\n")):
        lines = block.split("\n")
        out.append((lines[0].removeprefix("event: "), "\n".join(l.removeprefix("data: ") for l in lines[1:])))
    return out


def test_chapter_prompt_contains_required_context(use_llm):
    llm = FakeLLM()
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=CONTEXT) as res:
        res.read()
    chapter_prompt = llm.prompts[0]
    for expected in ["약속을 어기면 대가를 치르는 세계", "한서윤", "부상", "두 번째 약속", "단서를 발견한다", "서윤이 상점을 찾았다"]:
        assert expected in chapter_prompt, expected


def test_creative_brief_form_and_future_outline_reach_generation(use_llm):
    llm = FakeLLM()
    context = {**CONTEXT, "project": {**PROJECT, "creativeBrief": "초능력 없이 추리", "form": "SHORT"},
               "storyOutline": [{"order": 3, "title": "마지막 증언", "beats": ["범인을 밝힌다"]}],
               "previousChapters": [{"number": 1, "summary": "첫 사건", "endingExcerpt": "열쇠를 건넸다"}]}
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=context) as response:
        assert "event: done" in response.read().decode()
    for expected in ("초능력 없이 추리", "SHORT", "판타지", "마지막 증언", "열쇠를 건넸다"):
        assert expected in llm.prompts[0]
    assert len(llm.prompts) == 2


def test_blank_combined_summary_cannot_complete_generation(use_llm):
    class BlankSummary(FakeLLM):
        async def structured(self, **kwargs):
            return ChapterAnalysis(summary="   ", issues=[])
    with use_llm(BlankSummary()).stream("POST", "/internal/generate/chapter", json=CONTEXT) as response:
        body = response.read().decode()
    assert "event: error" in body
    assert "event: done" not in body


def test_continuity_prompt_contains_content_and_settings(use_llm):
    llm = FakeLLM()
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=CONTEXT) as res:
        res.read()
    continuity_prompt = llm.prompts[-1]
    for expected in ["본문 한서윤이 걸었다.", "약속을 어기면 대가를 치르는 세계", "단서의 행방", "서윤이 상점을 찾았다"]:
        assert expected in continuity_prompt, expected


def test_llm_continuity_issues_are_kept_and_lower_score(use_llm):
    issue = ContinuityIssue(type="timeline", description="어긋남", severity="high")
    with use_llm(FakeLLM(issues=[issue])).stream("POST", "/internal/generate/chapter", json=CONTEXT) as res:
        body = res.read().decode()
    assert '"어긋남"' in _events(body)[-1][1]
    assert '"score":66' in _events(body)[-1][1]  # high 30 + 짧은 본문 low 4


def test_llm_failure_mid_stream_sends_error_event_without_done(use_llm):
    llm = FakeLLM(stream_error=LLMError("모델이 요청을 거절했습니다."))
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=CONTEXT) as res:
        kinds = [k for k, _ in _events(res.read().decode())]
    assert kinds == ["token", "error"]


def test_unexpected_failure_hides_internal_message(use_llm):
    llm = FakeLLM(stream_error=RuntimeError("secret internal detail"))
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=CONTEXT) as res:
        body = res.read().decode()
    assert "event: error" in body
    assert "secret" not in body


def test_project_model_is_used_for_every_llm_call(use_llm):
    llm = FakeLLM()
    context = {**CONTEXT, "project": {**PROJECT, "model": "claude-sonnet-5-5"}}
    with use_llm(llm).stream("POST", "/internal/generate/chapter", json=context) as res:
        res.read()
    assert llm.models == ["claude-sonnet-5-5"] * 2  # 본문, 요약과 Continuity 공동 분석


def test_default_tone_reaches_bible_prompt(use_llm):
    llm = FakeLLM()
    project = {**PROJECT, "defaultTone": "건조한 하드보일드"}
    use_llm(llm).post("/internal/generate/bible", json={"project": project, "prompt": "p"})
    assert "건조한 하드보일드" in llm.prompts[0]


def test_haiku_omits_server_side_fallback():
    from app.core.config import Settings
    from app.core.llm import AnthropicLLM

    llm = AnthropicLLM(Settings(llm_provider="anthropic", anthropic_api_key="test"))
    assert "fallbacks" not in llm._params("claude-haiku-5-5")
    assert llm._params(None)["fallbacks"] == "default"
    assert llm._params("claude-sonnet-5-5")["model"] == "claude-sonnet-5-5"


def test_outline_is_renumbered(use_llm):
    res = use_llm(FakeLLM()).post(
        "/internal/generate/outline", json={"project": PROJECT, "bible": BIBLE, "chapterCount": 2}
    )
    assert [c["order"] for c in res.json()["chapters"]] == [1, 2]


def test_outline_rejects_wrong_chapter_count(use_llm):
    res = use_llm(FakeLLM()).post(
        "/internal/generate/outline", json={"project": PROJECT, "bible": BIBLE, "chapterCount": 3}
    )
    assert res.status_code == 502


def test_llm_error_on_sync_call_gives_502(use_llm):
    class Failing(FakeLLM):
        async def structured(self, *, system, prompt, schema, model=None):
            raise LLMError("실패")

    res = use_llm(Failing()).post("/internal/generate/bible", json={"project": PROJECT, "prompt": "p"})
    assert res.status_code == 502


@pytest.mark.parametrize("reason", ["refusal", "max_tokens"])
def test_stop_reasons_raise(reason):
    with pytest.raises(LLMError):
        _check_stop(reason)
    _check_stop("end_turn")


def test_every_prompt_template_renders():
    assert "장편 소설" in render("system")
    assert "문단은 빈 줄로" in render(
        "chapter",
        project=type("P", (), {"title": "t"}),
        bible=type("B", (), {"world_setting": "", "theme": "", "tone": "", "characters": []}),
        chapter_number=1,
        previous_chapters=[],
        outline=type("O", (), {"title": "t", "beats": [], "characters": [], "goals": "", "continuity_points": []}),
    )
