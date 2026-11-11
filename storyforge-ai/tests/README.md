# storyforge-ai/tests/

AI 서비스 테스트입니다. `LLM_PROVIDER=mock`(기본값)으로 실행하므로 API 키가 필요 없습니다.

총 72건이며 실제 외부 모델 호출 없이 실행합니다.

## 들어갈 것

- `test_chapter_scenes.py` — 구간별 사건 배분·앞 구간 실제 본문 전달·첫째와 둘째 구간 취소 즉시 해제
- `test_repetition_guard.py` — 긴 문장 및 구분 없는 반복 루프 차단, 짧은 대화 허용

- `test_ollama.py` — 연결 재사용, JSON 스키마·출력 잘림, 스트림 완료·취소, 동시성·대기 제한, 공백뿐인 스트림 거부
- `test_character_bank.py` — 인물 이름 자원과 중복 방지
- `test_pipeline.py` — 내부 API 계약(camelCase), 챕터 SSE 이벤트 순서(token → done), 서비스·SSE 종료 시 내부 모델 스트림 해제, Continuity 규칙 검사와 점수
- `test_guide.py` — 가이드챗 명령어, 자유 질문, 모델 응답이 이동 버튼을 만들지 못함
- `test_package_continuity.py` — 패키지 규칙 검사, 모든 작품이 프롬프트에 들어감
- `test_llm_paths.py` — 가짜 LLM으로 필수 컨텍스트 주입, LLM 검증 결과 반영, 스트림 중 오류, 내부 메시지 비노출 (실제 API 호출 없음)
- `test_validation_tools.py` — 예열만으로 0회 성공 방지, scripts 모듈에서 이름 풀을 읽고 mock 다중 챕터 검증까지 완료
- `test_orchestration.py` — 진입점 호환·API 경로 계약, 예열 실패 후 서비스 유지·종료 정리, 도구 선택·인자 전달·종료 시 복원

`conftest.py`는 `tests.orchestration` 플러그인을 등록합니다. 인물 저장소 격리 fixture는 그 파일에서 관리하며 모든 테스트에 자동 적용합니다.

## 실행

```
.venv/Scripts/python -m pip install -r requirements-dev.txt
.venv/Scripts/python -m pytest -q
.venv/Scripts/pyright --project ..
```

## 들어가지 않을 것

- 실제 Claude API를 호출하는 테스트 (비용이 들므로 별도 표시와 사용자 승인 후 추가)

## 관련 문서

