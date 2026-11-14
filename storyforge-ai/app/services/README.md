# storyforge-ai/app/services/

**AI 파이프라인 단계별 서비스**를 둡니다. 프롬프트에 컨텍스트를 주입하고 LLM을 호출합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `chapter_analysis.py` | 실모델 본문 요약과 연속성 판단을 한 구조화 요청으로 수행하고 규칙 검사 합산 |
| `repetition_guard.py` | 긴 문장 세 번 반복·구분 없는 반복 루프를 스트리밍 중 감지 |
| `orchestration.py` | `AiTaskOrchestrator`: Bible·Outline·챕터·수정본 검증·이어쓰기·패키지·가이드의 공통 진입점. 챕터 본문 → 요약 → Continuity → 최종 결과 순서와 중단 처리 |
| `bible_generator.py` | Story Bible 생성 |
| `outline_generator.py` | Outline 생성, order 재번호 |
| `chapter_generator.py` | 본문 스트림·두 구간 사건 배분·이어쓰기. 실제 앞 구간 본문을 다음 구간에 전달하며 생성 전체는 `orchestration.py`에서 관리 |
| `continuity_checker.py` | 규칙 기반 검사 + LLM 검증, 점수 계산. 직전 장면과 첫머리가 끊기면 `plot_thread`를 추가 |
| `story_scout.py` | 장면 개척. 직전 문장의 사실(`carry`)과 이어 쓸 첫 문단(`opening`). 저장·승인은 하지 않음 |
| `plausibility.py` | 장면 연결 로지스틱 회귀. 가중치는 `plausibility_weights.json`. 재학습은 `python -m scripts.train_plausibility` |
| `character_bank.py` | 이름 풀과 ChromaDB `characters` 컬렉션. 벡터가 없어도 JSON 중복 방지는 동작 |
| `package_continuity.py` | 패키지 작품 사이의 규칙 검사 + LLM 검증 ([규칙](../../../docs/novel/continuity-rules.md)) |
| `guide_assistant.py` | AI 가이드챗: 명령어 안내, 키워드 매칭, 자유 질문. 이동 버튼은 이 파일의 표에서만 만듦 |
| `mock_writer.py` | `LLM_PROVIDER=mock`용 결정적 생성기 (mock 표시 포함) |

위 표의 생성·검증 파일이 기획서 §6.1~§6.3의 Bible, Outline, 챕터, Continuity를 맡습니다. 캐릭터 상태 자동 갱신은 Phase 2라 이 폴더에 없습니다.

## 들어가지 않을 것

- DB 접근, 상태 전이 (Spring Boot 담당)
- 개별 생성기에서 API 계층이나 `AiTaskOrchestrator`를 역참조하는 순환 호출
- Continuity 실패를 임의로 통과시키는 로직

## 관련 문서

- [기획서 §6, §7.2, §8.3](../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/novel/](../../../docs/novel)
