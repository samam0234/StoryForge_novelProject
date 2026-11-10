"""검증 도구가 모델 예열만으로 성공했다고 보고하지 않는지 확인합니다."""
import asyncio
import json

from app.core.ollama import LLMBusyError
from app.models.schemas import ChapterGenerateResult, ContinuityResult
from app.models.orchestration import Done, Token
from app.core.config import get_settings
from app.core.llm import MockLLM
from scripts import validate_pipeline, validate_soak


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
    monkeypatch.setattr(validate_soak.AiTaskOrchestrator, "generate_chapter", generate)
    monkeypatch.setattr(validate_soak.time, "perf_counter", lambda: clock[0])

    assert asyncio.run(validate_soak.validate(duration=1, timeout=1)) == 0
    summary = json.loads(capsys.readouterr().out.splitlines()[-1])
    assert summary["rounds"] == summary["completed"] == 1
    assert summary["busy_rejected"] == summary["cancelled"] == 1
    assert closed == [True]


def test_pipeline_module_loads_name_pool_and_finishes_without_real_model(monkeypatch, capsys):
    closed = []

    class LocalMock(MockLLM):
        def __init__(self, settings):
            pass

        async def warmup(self):
            pass

        async def aclose(self):
            closed.append(True)

    monkeypatch.setattr(validate_pipeline, "OllamaLLM", LocalMock)
    monkeypatch.setattr(get_settings(), "mock_stream_delay", 0)
    asyncio.run(validate_pipeline.validate(chapters=2, timeout=5))

    stages = [json.loads(line) for line in capsys.readouterr().out.splitlines()]
    assert [stage["stage"] for stage in stages] == ["bible", "outline", "chapter_1", "chapter_2", "complete"]
    assert stages[-1]["chapters"] == 2
    assert closed == [True]
