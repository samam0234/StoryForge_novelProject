from pathlib import Path

from app.services.character_bank import CharacterBank


def test_name_pool_has_nine_countries_and_unique_hangul_names():
    bank = CharacterBank(Path(__file__).resolve().parents[1] / "data")
    rows = bank.all_names()
    names = [row["name"] for row in rows]
    countries = {row["country"] for row in rows}
    assert len(names) >= 200
    assert len(names) == len(set(names))
    assert countries == {"한국", "일본", "중국", "미국", "영국", "독일", "프랑스", "인도", "사우디아라비아"}
    assert all("a" not in name and "A" not in name for name in names)


def test_reserve_does_not_repeat_a_used_name(tmp_path: Path):
    source = Path(__file__).resolve().parents[1] / "data" / "names.json"
    (tmp_path / "names.json").write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
    bank = CharacterBank(tmp_path)
    first = bank.reserve(4)
    bank.commit([type("C", (), {"name": row["name"], "description": "설명", "personality": "성격"})() for row in first], "작품")
    second = bank.reserve(4)
    assert {row["name"] for row in first}.isdisjoint({row["name"] for row in second})
