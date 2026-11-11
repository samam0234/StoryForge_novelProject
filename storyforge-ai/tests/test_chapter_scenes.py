import asyncio
from contextlib import aclosing

import pytest

from app.core.llm import MockLLM
from app.models.schemas import ChapterContext
from app.services.chapter_generator import stream_body


def context():
    return ChapterContext.model_validate({"project": {"title": "등대", "genre": "미스터리"},
        "bible": {}, "chapterNumber": 2,
        "outlineChapter": {"order": 2, "title": "열쇠", "beats": ["발견", "선택", "대가", "확인"]}})


class SceneLLM(MockLLM):
    is_mock = False
    def __init__(self):
        self.prompts = []
        self.closed = []
    async def stream(self, *, system, prompt, model=None):
        self.prompts.append(prompt)
        number = len(self.prompts)
        try:
            yield f"새로운 사건 {number}."
        finally:
            self.closed.append(number)


def test_second_scene_receives_actual_first_scene_and_only_remaining_beats():
    llm = SceneLLM()
    async def run():
        return "".join([part async for part in stream_body(llm, context())])
    assert asyncio.run(run()) == "새로운 사건 1.\n\n새로운 사건 2."
    assert "새로운 사건 1." in llm.prompts[1]
    assert "- 발견" in llm.prompts[0] and "- 대가" not in llm.prompts[0]
    assert "- 대가" in llm.prompts[1] and "- 발견" not in llm.prompts[1]
    assert llm.closed == [1, 2]


@pytest.mark.parametrize("scene", [1, 2])
def test_cancellation_closes_active_scene_immediately(scene):
    llm = SceneLLM()
    async def run():
        async with aclosing(stream_body(llm, context())) as stream:
            async for part in stream:
                if part == f"새로운 사건 {scene}.":
                    break
        assert llm.closed == list(range(1, scene + 1))
        assert len(llm.prompts) == scene
    asyncio.run(run())
