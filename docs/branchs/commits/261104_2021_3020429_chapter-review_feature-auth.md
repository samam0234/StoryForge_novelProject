## 제목 / 커밋번호

- **제목**: `feat(api): 고친 원고를 승인할 때 다시 요약·검증`
- **커밋번호**: `3020429`
- **브랜치**: `feature/auth`
- **일시**: 2026-11-04 20:21

## 주 내용

사람이 고친 원고를 승인할 때 요약과 Continuity를 다시 만듭니다. 오래된 요약이 다음 챕터로 넘어가 설정이 무너지는 것을 막기 위함입니다.

## 상세

- V7, `Chapter.needsReview`, `PipelineOrchestrator.confirmChapter`, `/internal/generate/review`
- 심각한 이슈는 저장 후 409로 한 번 멈춤, AI 실패는 502로 승인하지 않음
- 상태 전이는 바꾸지 않음. 기획서 §6.3 Continuity 원칙을 따름

## 결과

- `ChapterReviewIntegrationTest` 5건 통과
- 실제 LLM 재요약 품질은 미검증 (mock)
- 이 기록은 2026-11-04에 소급 작성했습니다.
