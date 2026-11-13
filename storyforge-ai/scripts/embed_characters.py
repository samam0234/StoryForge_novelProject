"""이름 풀을 ChromaDB에 임베딩합니다. 모델 다운로드는 하지 않습니다."""
from app.services.character_bank import CharacterBank


def main() -> None:
    count = CharacterBank().embed_catalog()
    print(f"characters collection count={count}")


if __name__ == "__main__":
    main()
