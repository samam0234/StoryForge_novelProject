"""mock 공급자로 내부 API 계약과 SSE 형식을 확인합니다."""
import json

from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app
from app.models.schemas import ChapterContext, ContinuityIssue
from app.services.continuity_checker import rule_checks, score

get_settings().mock_stream_delay = 0
client = TestClient(app)

PROJECT = {"title": "테스트", "genre": "판타지"}


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
