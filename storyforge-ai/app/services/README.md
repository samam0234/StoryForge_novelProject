# storyforge-ai/app/services/

**AI 파이프라인 단계별 서비스**를 둡니다. 프롬프트에 컨텍스트를 주입하고 LLM을 호출합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `bible_generator.py` | Story Bible 생성 |
| `outline_generator.py` | Outline 생성, order 재번호 |
| `chapter_generator.py` | 본문 스트리밍 → 요약 → Continuity 검증 → 결과 |
| `continuity_checker.py` | 규칙 기반 검사 + LLM 검증, 점수 계산 |
| `mock_writer.py` | `LLM_PROVIDER=mock`용 결정적 생성기 (mock 표시 포함) |

## 들어갈 것

- `bible_generator.py` — Story Bible 초기 생성 (§6.1)
- `outline_generator.py` — Outline 생성·부분 재생성 (§6.2)
- `chapter_generator.py` — 챕터 생성. 컨텍스트: Bible 관련 항목 + Outline + 이전 챕터 요약 + 캐릭터 상태 (§6.3)
- `continuity_checker.py` — 설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드 검증 (§6.3)

## 들어가지 않을 것

- DB 접근, 상태 전이 (Spring Boot 담당)
- Continuity 실패를 임의로 통과시키는 로직

## 관련 문서

- [기획서 §6, §7.2, §8.3](../../../StoryForge_기획서_v1.2.md)
- [docs/novel/](../../../docs/novel)
- 상위 폴더: [../README.md](../README.md)
