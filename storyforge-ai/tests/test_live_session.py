"""개연 모델, 장면 개척, 내부 웹소켓."""
import json

from fastapi.testclient import TestClient

from app.models.schemas import BibleInfo, ChapterContext, OutlineChapter, ProjectInfo, ScoutRequest
from app.services.continuity_checker import rule_checks
from app.services.plausibility import awkward_probability, is_awkward
from app.services.story_scout import rule_directions
from main import create_app

PROJECT = {"title": "기억 상점", "genre": "미스터리", "model": "mock"}


def test_smooth_opening_is_not_awkward_and_a_time_jump_is():
    previous = "문을 열고 안을 들여다보았다"
    assert awkward_probability(previous, "그리고 문 너머에는 꺼진 등 하나만 남아 있었다") < 0.5
    assert is_awkward(previous, "갑자기 몇 년 뒤 그는 사막의 우주선에서 눈을 떴다")


def test_short_previous_scene_is_not_scored():
    assert awkward_probability("짧다", "갑자기 다른 세계") == 0.0


def test_rule_scout_stays_inside_the_previous_action_and_known_names():
    request = ScoutRequest(
        project=ProjectInfo(title="기억 상점", genre="미스터리"),
        bible=BibleInfo(world_setting="약속에는 대가가 있다", characters=[]),
        outline_chapter=OutlineChapter(order=2, title="전표", beats=["수신인을 확인한다"], characters=["한서윤"]),
        previous_ending="전표를 찾아 주머니에 넣었다",
    )
    directions = rule_directions(request)
    assert len(directions) == 3
    assert "전표를 찾아 주머니에 넣었다" in directions[0].causal_link
    assert "수신인을 확인한다" in directions[0].next_action
    assert directions[0].carry
    assert any("전표" in fact for fact in directions[0].carry)
    assert "수신인을 확인한다" in directions[0].opening
    assert all("없는 인물" in item.avoid for item in directions)


def test_bridge_issue_uses_existing_plot_thread_type():
    ctx = ChapterContext.model_validate({
        "project": PROJECT,
        "bible": {"characters": [{"name": "한서윤"}]},
        "chapterNumber": 2,
        "outlineChapter": {"order": 2, "title": "t", "characters": ["한서윤"]},
        "previousChapters": [{
            "number": 1,
            "summary": "전표를 찾았다",
            "endingExcerpt": "문을 열고 안을 들여다보았다",
        }],
    })
    broken = rule_checks(ctx, "한서윤은 갑자기 몇 년 뒤 사막의 우주선에서 눈을 떴다. " + "가" * 4000)
    assert any("인과가 끊겨" in issue.description and issue.type == "plot_thread" for issue in broken)
    linked = rule_checks(ctx, "한서윤은 그리고 문 너머에는 꺼진 등 하나만 남아 있었다. " + "가" * 4000)
    assert not any("인과가 끊겨" in issue.description for issue in linked)


def test_internal_websocket_keeps_the_session_for_a_second_request():
    client = TestClient(create_app())
    with client.websocket_connect("/internal/ws") as socket:
        socket.send_text(json.dumps({"op": "ping", "id": "1"}))
        assert socket.receive_json() == {"op": "pong", "id": "1"}
        socket.send_text(json.dumps({
            "op": "scout",
            "id": "2",
            "project": PROJECT,
            "bible": {"worldSetting": "약속", "characters": [{"name": "한서윤"}]},
            "outlineChapter": {"order": 1, "title": "도입", "beats": ["약속을 확인한다"], "characters": ["한서윤"]},
            "previousEnding": "문을 열고 안을 들여다보았다",
        }))
        body = socket.receive_json()
        assert body["op"] == "scout" and body["id"] == "2"
        assert len(body["directions"]) == 3
        assert "문을 열고 안을 들여다보았다" in body["directions"][0]["causalLink"]
        assert body["directions"][0]["carry"]
        assert body["directions"][0]["opening"]
