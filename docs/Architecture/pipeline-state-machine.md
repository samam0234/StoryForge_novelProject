# 파이프라인 상태 머신 (프로젝트 단위)

> 기준: 기획서 v1.2 §7.2 — **임의 변경 금지** (정본 §0, "상태 머신")
> 상태 이름과 순서는 원본 그대로입니다. 바꾸려면 먼저 사용자 승인을 받고 기획서 버전을 올립니다.

## 상태 순서

```
BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED
→ CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED
```

```mermaid
stateDiagram-v2
    [*] --> BIBLE_DRAFT
    BIBLE_DRAFT --> BIBLE_CONFIRMED
    BIBLE_CONFIRMED --> OUTLINE_DRAFT
    OUTLINE_DRAFT --> OUTLINE_CONFIRMED
    OUTLINE_CONFIRMED --> CHAPTER_GENERATING
    CHAPTER_GENERATING --> CHAPTER_REVIEW
    CHAPTER_REVIEW --> COMPLETED
    COMPLETED --> [*]
```

## 상태별 의미와 담당

| 상태 | 의미 | 상태를 바꾸는 주체 |
|------|------|--------------------|
| `BIBLE_DRAFT` | Story Bible 생성·편집 중 | Spring Boot (사용자 요청 → FastAPI 생성) |
| `BIBLE_CONFIRMED` | 사용자가 Story Bible을 확정 | Spring Boot |
| `OUTLINE_DRAFT` | Outline 생성·편집 중 | Spring Boot |
| `OUTLINE_CONFIRMED` | 사용자가 Outline을 확정 | Spring Boot |
| `CHAPTER_GENERATING` | 챕터 생성 + Continuity 검증 진행 중 | Spring Boot (생성은 FastAPI) |
| `CHAPTER_REVIEW` | 사용자 검토 대기. **건너뛰지 않습니다** | Spring Boot |
| `COMPLETED` | 프로젝트 완료 | Spring Boot |

상태 전이는 Spring Boot `service/orchestration/`에서만 일어납니다. Angular와 FastAPI는 상태를 바꾸지 않습니다.

## 별도 상태: Chapter (§10)

프로젝트 상태와 별개로, 각 Chapter 엔티티는 `DRAFT` / `CONFIRMED` 상태를 가집니다.
§7.2 6단계에 따라 사용자가 승인하면 Chapter가 `CONFIRMED`로 바뀌고 프로젝트 상태 머신이 갱신됩니다.

## 미정 사항 (기획서에 정의 없음 — 사용자 확인 필요)

아래 항목은 원본에 정의되어 있지 않아 다이어그램에 넣지 않았습니다. 결정되면 기획서 개정 또는 이 절 아래 추가로 반영합니다.

- 여러 챕터를 순차 생성할 때 `CHAPTER_REVIEW` → 다음 챕터 `CHAPTER_GENERATING`으로 돌아가는 전이 여부
- 확정 후 되돌리기 (예: `BIBLE_CONFIRMED` → `BIBLE_DRAFT`) 허용 여부
- 생성 실패, 타임아웃 등 오류 상태 처리 방식

### 구현 임시안 (2026-10-22, 확인 대기)

첫 구현을 동작시키려면 위 항목 중 두 가지가 필요해서, 아래 **임시 전이**만 추가했습니다.
코드에서는 `PipelineStateMachine`의 `PROVISIONAL` 표에 기획서 전이(`DEFINED`)와 분리해 두었습니다. 승인되면 기획서에 반영하고, 거절되면 이 표만 지우면 됩니다.

| 전이 | 쓰임 |
|------|------|
| `CHAPTER_REVIEW → CHAPTER_GENERATING` | 챕터 N 승인 후 N+1장 생성, 검토 중 같은 챕터 재생성 |
| `CHAPTER_GENERATING → OUTLINE_CONFIRMED` | 첫 챕터 생성 실패·중단 시 생성 전 상태로 복귀 |

- 두 번째 이후 챕터 생성이 실패하면 `CHAPTER_GENERATING → CHAPTER_REVIEW`(기획서에 있는 전이)로 돌아갑니다.
- 확정 취소(되돌리기)는 구현하지 않았습니다. 확정 후에는 Bible·Outline을 수정할 수 없습니다.
- 서버가 생성 도중 종료되면 프로젝트가 `CHAPTER_GENERATING`에 남을 수 있습니다. 복구 방법은 아직 없습니다.

```mermaid
stateDiagram-v2
    OUTLINE_CONFIRMED --> CHAPTER_GENERATING
    CHAPTER_GENERATING --> CHAPTER_REVIEW
    CHAPTER_REVIEW --> COMPLETED: 마지막 챕터 승인
    CHAPTER_REVIEW --> CHAPTER_GENERATING: 임시 — 다음 챕터 / 재생성
    CHAPTER_GENERATING --> OUTLINE_CONFIRMED: 임시 — 첫 챕터 실패
```
