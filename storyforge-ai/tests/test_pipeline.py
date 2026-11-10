"""mock 공급자로 내부 API 계약과 SSE 형식을 확인합니다."""
import json

from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app
from app.models.schemas import ChapterContext, ContinuityIssue
from app.services import mock_writer
from app.services.continuity_checker import rule_checks, score

get_settings().mock_stream_delay = 0
client = TestClient(app)

PROJECT = {"title": "테스트", "genre": "판타지"}


def test_complete_does_not_echo_the_previous_sentence():
    res = client.post(
        "/internal/generate/complete",
        json={"before": "한서윤은 이미 상점 문을 닫았다.", "after": ""},
    )
    assert res.status_code == 200
    suggestion = res.json()["suggestion"]
    assert suggestion.strip()
    assert "상점 문을 닫았다" not in suggestion


def test_bible_and_outline_use_camel_case_contract():
    bible = client.post("/internal/generate/bible", json={"project": PROJECT, "prompt": "기억을 파는 상점"}).json()
    assert {"worldSetting", "theme", "tone", "characters"} <= bible.keys()

    outline = client.post(
        "/internal/generate/outline",
        json={"project": PROJECT, "bible": bible, "chapterCount": 4},
    ).json()
    assert [c["order"] for c in outline["chapters"]] == [1, 2, 3, 4]
    assert "continuityPoints" in outline["chapters"][0]


def test_chapter_streams_tokens_then_done_with_report():
    bible = client.post("/internal/generate/bible", json={"project": PROJECT, "prompt": "x"}).json()
    outline = client.post(
        "/internal/generate/outline", json={"project": PROJECT, "bible": bible, "chapterCount": 2}
    ).json()
    ctx = {
        "project": PROJECT,
        "bible": bible,
        "chapterNumber": 1,
        "outlineChapter": outline["chapters"][0],
        "previousChapters": [],
    }
    with client.stream("POST", "/internal/generate/chapter", json=ctx) as res:
        body = res.read().decode()
    events = [b for b in body.split("\n\n") if b]
    kinds = [b.split("\n")[0].removeprefix("event: ") for b in events]
    assert kinds[0] == "token" and kinds[-1] == "done"

    done = json.loads("\n".join(l.removeprefix("data: ") for l in events[-1].split("\n")[1:]))
    assert done["title"] == outline["chapters"][0]["title"]
    assert 0 <= done["report"]["score"] <= 100
    assert done["summary"]


def test_rule_checks_flag_missing_character_and_lower_score():
    ctx = ChapterContext.model_validate(
        {
            "project": PROJECT,
            "bible": {"characters": [{"name": "한서윤"}]},
            "chapterNumber": 1,
            "outlineChapter": {"order": 1, "title": "t", "characters": ["한서윤", "유령"]},
        }
    )
    issues = rule_checks(ctx, "한서윤이 걸었다." * 50)
    assert any("유령" in i.description for i in issues)
    assert score(issues) < 100
    assert score([ContinuityIssue(type="timeline", description="d", severity="high")]) == 70


def test_review_uses_edited_text_for_summary_and_rules():
    ctx = {
        "project": PROJECT,
        "bible": {"characters": [{"name": "한서윤"}]},
        "chapterNumber": 2,
        "outlineChapter": {"order": 2, "title": "t", "characters": ["한서윤"]},
    }
    missing = client.post("/internal/generate/review", json={"context": ctx, "title": "t", "content": "고친 본문"}).json()
    assert missing["summary"] == "2장(수정본): 고친 본문"
    assert any("나오지 않습니다" in i["description"] for i in missing["report"]["issues"])

    present = client.post(
        "/internal/generate/review", json={"context": ctx, "title": "t", "content": "한서윤이 돌아왔다." * 40}
    ).json()
    assert present["summary"].startswith("2장(수정본): 한서윤")
    assert len(present["report"]["issues"]) == 1
    assert "4,000자" in present["report"]["issues"][0]["description"]
    assert present["report"]["score"] == 96


def test_length_warning_uses_chapter_target_and_name_warning_is_not_a_presence_claim():
    ctx = ChapterContext.model_validate({
        "project": PROJECT, "bible": {"characters": [{"name": "한서윤"}]}, "chapterNumber": 1,
        "outlineChapter": {"order": 1, "title": "t", "characters": ["한서윤"]},
    })
    short = rule_checks(ctx, "서윤은 창가에 앉았다." + "가" * 300)
    assert any("전체 이름" in i.description and "다른 호칭" in i.description for i in short)
    assert any("4,000자" in i.description for i in short)
    exact = "한서윤" + "가" * 3997
    assert not any("4,000자" in i.description for i in rule_checks(ctx, exact))


def test_later_mock_chapter_does_not_reuse_the_previous_scene():
    bible = client.post("/internal/generate/bible", json={"project": PROJECT, "prompt": "x"}).json()
    outline = client.post(
        "/internal/generate/outline", json={"project": PROJECT, "bible": bible, "chapterCount": 3}
    ).json()
    first = ChapterContext.model_validate(
        {"project": PROJECT, "bible": bible, "chapterNumber": 1, "outlineChapter": outline["chapters"][0]}
    )
    second = ChapterContext.model_validate(
        {
            "project": PROJECT,
            "bible": bible,
            "chapterNumber": 2,
            "outlineChapter": outline["chapters"][1],
            "previousChapters": [{"number": 1, "title": "도입", "summary": "서윤이 새벽 도매시장에서 전표를 찾았다"}],
        }
    )
    opening = mock_writer.chapter_text(first)
    follow = mock_writer.chapter_text(second)
    assert "지난 일이 아직 끝나지 않았다" not in follow
    assert "서윤이 새벽 도매시장에서 전표를 찾았다" not in follow
    assert opening.split("\n\n")[1] not in follow
    repeated = rule_checks(second, follow)
    assert not any("그대로" in issue.description for issue in repeated)

