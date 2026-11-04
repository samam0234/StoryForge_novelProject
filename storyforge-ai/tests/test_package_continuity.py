"""패키지 전체 Continuity 검사"""
from fastapi.testclient import TestClient

from app.core.llm import get_llm
from app.main import app
from app.models.schemas import PackageIssue, PackageIssues

client = TestClient(app)


def work(title: str, description: str) -> dict:
    return {
        "title": title,
        "role": "MAIN",
        "bible": {"characters": [{"name": "한서윤", "description": description}]},
        "chapters": [{"number": 1, "title": "t", "summary": "요약"}],
    }


def check(body: dict) -> dict:
    res = client.post("/internal/package/continuity", json=body)
    assert res.status_code == 200
    return res.json()


def test_same_name_with_different_description_is_flagged():
    data = check({"title": "묶음", "works": [work("가", "왼손 화상"), work("나", "오른손 의수")]})
    assert data["issues"][0]["type"] == "character_consistency"
    assert data["issues"][0]["workTitles"] == ["가", "나"]
    assert data["score"] == 88


def test_same_description_passes():
    data = check({"title": "묶음", "works": [work("가", "왼손 화상"), work("나", "왼손 화상")]})
    assert data == {"issues": [], "score": 100}


def test_unlinked_work_is_noted_only_when_links_exist():
    links = [{"fromTitle": "가", "toTitle": "나", "type": "CONTINUES"}]
    body = {"title": "묶음", "works": [work("가", "x"), work("나", "x"), work("다", "x")], "links": links}
    data = check(body)
    assert [i["workTitles"] for i in data["issues"]] == [["다"]]
    assert data["issues"][0]["severity"] == "low"


class FakeLLM:
    is_mock = False

    def __init__(self) -> None:
        self.prompt = ""

    async def structured(self, *, system: str, prompt: str, schema, model=None):
        self.prompt = prompt
        return PackageIssues(
            issues=[PackageIssue(type="timeline", description="연표 충돌", severity="high", work_titles=["가", "나"])]
        )


def test_llm_receives_every_work_and_adds_issues():
    fake = FakeLLM()
    app.dependency_overrides[get_llm] = lambda: fake
    try:
        data = check(
            {
                "title": "묶음",
                "worldNote": "약속을 어기면 기억을 잃는다",
                "works": [work("가", "x"), work("나", "x")],
                "links": [{"fromTitle": "가", "toTitle": "나", "type": "SAME_EVENT", "label": "3장 사건"}],
            }
        )
    finally:
        app.dependency_overrides.clear()
    assert data["issues"][0]["description"] == "연표 충돌"
    assert data["score"] == 70
    for text in ("약속을 어기면", 'title="가"', 'title="나"', "3장 사건", "요약"):
        assert text in fake.prompt
