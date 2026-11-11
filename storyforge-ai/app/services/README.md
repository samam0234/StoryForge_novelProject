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
| `chapter_generator.py` | 본문 스트림·요약·이어쓰기의 개별 작업. 실행 순서는 `orchestration.py`에서 관리 |
| `continuity_checker.py` | 규칙 기반 검사 + LLM 검증, 점수 계산 |
| `package_continuity.py` | 패키지 작품 사이의 규칙 검사 + LLM 검증 ([규칙](../../../docs/novel/continuity-rules.md)) |
| `guide_assistant.py` | AI 가이드챗: 명령어 안내, 키워드 매칭, 자유 질문. 이동 버튼은 이 파일의 표에서만 만듦 |
| `mock_writer.py` | `LLM_PROVIDER=mock`용 결정적 생성기 (mock 표시 포함) |

## 들어갈 것

- `bible_generator.py` — Story Bible 초기 생성 (§6.1)
- `outline_generator.py` — Outline 생성·부분 재생성 (§6.2)
- `chapter_generator.py` — 챕터 생성. 컨텍스트: Bible 관련 항목 + Outline + 이전 챕터 요약 + 캐릭터 상태 (§6.3)
- `continuity_checker.py` — 설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드 검증 (§6.3)

## 들어가지 않을 것

- DB 접근, 상태 전이 (Spring Boot 담당)
- 개별 생성기에서 API 계층이나 `AiTaskOrchestrator`를 역참조하는 순환 호출
- Continuity 실패를 임의로 통과시키는 로직

## 관련 문서

- [기획서 §6, §7.2, §8.3](../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/novel/](../../../docs/novel)
