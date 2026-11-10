"""검증 도구가 모델 예열만으로 성공했다고 보고하지 않는지 확인합니다."""
import asyncio
import json

from app.core.ollama import LLMBusyError
from app.models.schemas import ChapterGenerateResult, ContinuityResult
from app.services.chapter_generator import Done, Token
import validate_soak


def test_soak_runs_a_generation_even_when_warmup_exceeds_duration(monkeypatch, capsys):
    clock = [0.0]
    closed = []

    class FakeLLM:
        def __init__(self, settings):
            pass

        async def warmup(self):
            clock[0] = 10.0

        async def text(self, **kwargs):
            raise LLMBusyError("검증용 슬롯 포화")

        async def aclose(self):
            closed.append(True)

    async def generate(llm, ctx):
        yield Token("본문")
        yield Done(ChapterGenerateResult(title="검증", content="본문", summary="요약",
                                        report=ContinuityResult(issues=[], score=100)))

    monkeypatch.setattr(validate_soak, "OllamaLLM", FakeLLM)
    monkeypatch.setattr(validate_soak, "generate_chapter", generate)
    monkeypatch.setattr(validate_soak.time, "perf_counter", lambda: clock[0])

    assert asyncio.run(validate_soak.validate(duration=1, timeout=1)) == 0
    summary = json.loads(capsys.readouterr().out.splitlines()[-1])
    assert summary["rounds"] == summary["completed"] == 1
    assert summary["busy_rejected"] == summary["cancelled"] == 1
    assert closed == [True]
