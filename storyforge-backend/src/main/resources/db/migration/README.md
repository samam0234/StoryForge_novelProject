# storyforge-backend/src/main/resources/db/migration/

PostgreSQL 스키마를 관리하는 **Flyway 마이그레이션 SQL**을 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `V1__init_schema.sql` | 기획서 §10 테이블 6개 |

## 들어갈 것

- `V{번호}__{설명}.sql` 형식의 마이그레이션 (예: `V1__init_schema.sql`)
- 스키마는 기획서 §10과 `docs/plan/`의 ERD 기준

## 들어가지 않을 것

- 이미 적용된 마이그레이션 파일 수정 — 새 버전 파일로 추가합니다
- 실유저 데이터

> 정본의 권장 구조 요약에는 `db/migration/`이 Java 패키지 옆에 적혀 있지만, 우선순위 1인 기획서 §8.2에 따라 `src/main/resources/db/migration/`에 두었습니다.

## 관련 문서

- [기획서 §8.2, §10, §12](../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
