"""등장인물 이름 풀과 사용 기록.

같은 이름은 작품이 달라도 다시 쓰지 않습니다.
설명은 ChromaDB에 넣어, 새 인물이 기존 인물과 너무 비슷하면 프롬프트에 피하라고 넣습니다.
"""
from __future__ import annotations

import json
from pathlib import Path


_ROOT = Path(__file__).resolve().parents[2] / "data"


class _OverlapEmbedding:
    """다운로드 없이 글자 겹침으로 비슷한 인물 설명을 찾습니다."""

    def __init__(self) -> None:
        self._dim = 256

    def name(self) -> str:
        return "storyforge-overlap"

    def embed_documents(self, input: list[str]) -> list[list[float]]:
        return [self._vec(text) for text in input]

    def embed_query(self, input: list[str]) -> list[list[float]]:
        return self.embed_documents(input)

    def __call__(self, input: list[str]) -> list[list[float]]:
        return self.embed_documents(input)

    def _vec(self, text: str) -> list[float]:
        vec = [0.0] * self._dim
        compact = "".join(text.split())
        if not compact:
            return vec
        for i, ch in enumerate(compact):
            vec[ord(ch) % self._dim] += 1.0
            if i:
                vec[(ord(compact[i - 1]) * 31 + ord(ch)) % self._dim] += 0.5
        norm = sum(v * v for v in vec) ** 0.5 or 1.0
        return [v / norm for v in vec]


class CharacterBank:
    def __init__(self, root: Path | None = None) -> None:
        base = root or _ROOT
        self.names_path = base / "names.json"
        self.used_path = base / "used_characters.json"
        self.chroma_path = base / "chroma"
        self._collection = None

    def all_names(self) -> list[dict]:
        return json.loads(self.names_path.read_text(encoding="utf-8"))

    def used(self) -> list[dict]:
        if not self.used_path.exists():
            return []
        return json.loads(self.used_path.read_text(encoding="utf-8"))

    def reserve(self, count: int) -> list[dict]:
        taken = {row["name"] for row in self.used()}
        fresh = [row for row in self.all_names() if row["name"] not in taken]
        if len(fresh) < count:
            raise RuntimeError("사용하지 않은 인물 이름이 부족합니다.")
        # 나라별로 흩어서 한 작품에 같은 문화권만 몰리지 않게 합니다.
        picked: list[dict] = []
        seen_countries: set[str] = set()
        for row in fresh:
            if row["country"] in seen_countries:
                continue
            picked.append(row)
            seen_countries.add(row["country"])
            if len(picked) == count:
                return picked
        for row in fresh:
            if row not in picked:
                picked.append(row)
            if len(picked) == count:
                break
        return picked

    def similar_notes(self, idea: str, limit: int = 3) -> list[str]:
        collection = self._collection_or_none()
        if collection is None or collection.count() == 0 or not idea.strip():
            return []
        found = collection.query(query_texts=[idea], n_results=min(limit, collection.count()))
        docs = (found.get("documents") or [[]])[0]
        distances = (found.get("distances") or [[]])[0]
        notes = []
        for doc, dist in zip(docs, distances):
            if doc and dist is not None and dist < 0.35:
                notes.append(doc)
        return notes

    def commit(self, characters: list, work_title: str) -> None:
        rows = self.used()
        taken = {row["name"] for row in rows}
        documents = []
        ids = []
        for character in characters:
            name = getattr(character, "name", "") or ""
            if not name or name in taken:
                continue
            text = f"{name} / {getattr(character, 'description', '') or ''} / {getattr(character, 'personality', '') or ''}"
            rows.append({"name": name, "work": work_title, "text": text})
            taken.add(name)
            documents.append(text)
            ids.append(f"{work_title}-{name}")
        self.used_path.parent.mkdir(parents=True, exist_ok=True)
        self.used_path.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
        collection = self._collection_or_none()
        if collection is not None and documents:
            collection.upsert(ids=ids, documents=documents)

    def embed_catalog(self) -> int:
        """이름 풀 전체를 Chroma 컬렉션 characters에 벡터로 넣습니다."""
        rows = self.all_names()
        collection = self._collection_or_none()
        if collection is None:
            raise RuntimeError("ChromaDB를 불러오지 못했습니다. pip install chromadb 후 다시 실행하세요.")
        ids = [f"pool-{index}" for index in range(len(rows))]
        documents = [f"{row['name']} / {row['country']}식 한글 이름" for row in rows]
        metadatas = [{"country": row["country"], "kind": "pool"} for row in rows]
        collection.upsert(ids=ids, documents=documents, metadatas=metadatas)
        return int(collection.count())

    def _collection_or_none(self):
        if self._collection is not None:
            return self._collection
        try:
            from importlib import import_module
            chromadb = import_module("chromadb")
        except Exception:
            return None
        try:
            self.chroma_path.mkdir(parents=True, exist_ok=True)
            client = chromadb.PersistentClient(path=str(self.chroma_path))
            self._collection = client.get_or_create_collection(
                name="characters",
                embedding_function=_OverlapEmbedding(),
            )
        except Exception:
            return None
        return self._collection
