# storyforge-ai/tests/

AI 서비스 테스트입니다. `LLM_PROVIDER=mock`(기본값)으로 실행하므로 API 키가 필요 없습니다.

총 56건이며 실제 외부 모델 호출 없이 실행합니다.

## 들어갈 것

- `test_ollama.py` — 연결 재사용, JSON 스키마·출력 잘림, 스트림 완료·취소, 동시성·대기 제한, 공백뿐인 스트림 거부
- `test_character_bank.py` — 인물 이름 자원과 중복 방지
- `test_pipeline.py` — 내부 API 계약(camelCase), 챕터 SSE 이벤트 순서(token → done), 서비스·SSE 종료 시 내부 모델 스트림 해제, Continuity 규칙 검사와 점수
- `test_guide.py` — 가이드챗 명령어, 자유 질문, 모델 응답이 이동 버튼을 만들지 못함
- `test_package_continuity.py` — 패키지 규칙 검사, 모든 작품이 프롬프트에 들어감
- `test_llm_paths.py` — 가짜 LLM으로 필수 컨텍스트 주입, LLM 검증 결과 반영, 스트림 중 오류, 내부 메시지 비노출 (실제 API 호출 없음)
- `test_validation_tools.py` — 예열이 시험 시간보다 길어도 실제 생성·검증을 한 번 수행하며 0회 성공으로 끝나지 않음

## 실행

```
.venv/Scripts/python -m pip install -r requirements-dev.txt
.venv/Scripts/python -m pytest -q
.venv/Scripts/pyright --project ..
```

## 들어가지 않을 것

- 실제 Claude API를 호출하는 테스트 (비용이 들므로 별도 표시와 사용자 승인 후 추가)

## 관련 문서

