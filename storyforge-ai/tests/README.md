# storyforge-ai/tests/

AI 서비스 테스트입니다. `LLM_PROVIDER=mock`(기본값)으로 실행하므로 API 키가 필요 없습니다.

## 들어갈 것

- `test_pipeline.py` — 내부 API 계약(camelCase), 챕터 SSE 이벤트 순서(token → done), Continuity 규칙 검사와 점수

## 실행

```
.venv/Scripts/python -m pip install -r requirements-dev.txt
.venv/Scripts/python -m pytest -q
```

## 들어가지 않을 것

- 실제 Claude API를 호출하는 테스트 (비용이 들므로 별도 표시와 사용자 승인 후 추가)

## 관련 문서

- 상위 폴더: [../README.md](../README.md)
