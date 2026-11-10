"""서버 조립·수명주기·도구 선택의 실행 경계를 검증합니다."""
import asyncio
from functools import lru_cache
import sys

from fastapi.testclient import TestClient
import pytest

import main
from app import main as legacy
from app.core import orchestration as runtime
from app.core.config import get_settings
from scripts import orchestration as tools


def test_entrypoints_share_app_and_factory_preserves_unique_contract():
    assert legacy.app is main.app
    other = main.create_app()
    assert other is not main.app
    def operations(app):
        return [(method.upper(), path) for path, methods in app.openapi()["paths"].items()
                for method in methods]
    actual = operations(other)
    assert len(actual) == len(set(actual))
    assert set(actual) == {("GET", "/health"), *( ("POST", path) for path in (
        "/internal/generate/bible", "/internal/generate/outline", "/internal/generate/chapter",
        "/internal/generate/review", "/internal/generate/complete",
        "/internal/package/continuity", "/internal/guide/chat"))}
    assert actual == operations(main.app)
    identifiers = [operation["operationId"] for methods in other.openapi()["paths"].values()
                   for operation in methods.values()]
    assert len(identifiers) == len(set(identifiers))


@pytest.mark.parametrize("warmup_fails", [False, True])
def test_runtime_warms_up_and_closes_even_after_optional_warmup_failure(monkeypatch, warmup_fails):
    calls = []
    class LocalRuntime(runtime.RoutingLLM):
        def __init__(self):
            pass
        async def warmup(self):
            calls.append("warmup")
            if warmup_fails:
                raise RuntimeError("offline model")
        async def aclose(self):
            calls.append("close")
    @lru_cache
    def get_local():
        return LocalRuntime()
    monkeypatch.setattr(runtime, "get_llm", get_local)
    monkeypatch.setattr(get_settings(), "llm_provider", "ollama")
    monkeypatch.setattr(get_settings(), "ollama_warmup", True)
    with TestClient(main.create_app()) as client:
        assert calls == ["warmup"]
        assert client.get("/health").json()["provider"] == "ollama"
    assert calls == ["warmup", "close"]
    assert get_local.cache_info().currsize == 0


def test_runtime_clears_cached_client_when_close_raises(monkeypatch):
    class BrokenClose(runtime.RoutingLLM):
        def __init__(self):
            pass
        async def aclose(self):
            raise RuntimeError("close failed")
    @lru_cache
    def get_local():
        return BrokenClose()
    monkeypatch.setattr(runtime, "get_llm", get_local)
    monkeypatch.setattr(get_settings(), "ollama_warmup", False)
    async def run():
        async with runtime.llm_runtime():
            assert get_local.cache_info().currsize == 1
    with pytest.raises(RuntimeError, match="close failed"):
        asyncio.run(run())
    assert get_local.cache_info().currsize == 0


@pytest.mark.parametrize("raises", [False, True])
def test_tool_dispatch_preserves_arguments_and_restores_argv(monkeypatch, raises):
    previous = sys.argv
    def run_module(name, *, run_name):
        assert name == "scripts.validate_pipeline" and run_name == "__main__"
        assert sys.argv == ["scripts.validate_pipeline", "--chapters", "2"]
        if raises:
            raise SystemExit(2)
    monkeypatch.setattr(tools.runpy, "run_module", run_module)
    if raises:
        with pytest.raises(SystemExit):
            tools.main(["validate_pipeline", "--chapters", "2"])
    else:
        tools.main(["validate_pipeline", "--chapters", "2"])
    assert sys.argv is previous


def test_tool_dispatch_rejects_unknown_modules_before_running(monkeypatch):
    def unexpected(*args, **kwargs):
        pytest.fail("unknown tool executed")
    monkeypatch.setattr(tools.runpy, "run_module", unexpected)
    with pytest.raises(SystemExit) as exc:
        tools.main(["not_a_tool"])
    assert exc.value.code == 2
