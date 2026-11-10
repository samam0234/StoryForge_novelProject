"""회귀 테스트가 개발자의 인물 사용 기록을 변경하지 않도록 격리합니다."""
from pathlib import Path

import pytest

from app.services import character_bank


@pytest.fixture(autouse=True)
def isolated_character_bank(tmp_path, monkeypatch):
    source = Path(__file__).resolve().parents[1] / "data" / "names.json"
    (tmp_path / "names.json").write_bytes(source.read_bytes())
    monkeypatch.setattr(character_bank, "_ROOT", tmp_path)
