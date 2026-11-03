# storyforge-backend/src/main/resources/db/migration/

PostgreSQL 스키마를 관리하는 **Flyway 마이그레이션 SQL**을 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `V1__init_schema.sql` | 기획서 §10 테이블 6개 |
| `V2__users_and_ownership.sql` | `app_user`, 프로젝트 소유자 FK |
| `V3__project_version.sql` | 프로젝트 낙관적 락 |
| `V4__project_settings_and_versions.sql` | 프로젝트 설정 컬럼, Bible·챕터 이력 테이블 |
| `V5__community.sql` | 게시 작품, Clone, 공유, 패키지, 게시글, 댓글, 활동 기록, 업적, 달성, 프로필 컬럼. 기존 챕터 이력을 활동 기록으로 옮김 |

## 들어갈 것

- `V{번호}__{설명}.sql` 형식의 마이그레이션 (예: `V1__init_schema.sql`)
- 스키마는 기획서 §10과 `docs/plan/`의 ERD 기준

## 들어가지 않을 것

- 이미 적용된 마이그레이션 파일 수정 — 새 버전 파일로 추가합니다
- 실유저 데이터

> 정본의 권장 구조 요약에는 `db/migration/`이 Java 패키지 옆에 적혀 있지만, 우선순위 1인 기획서 §8.2에 따라 `src/main/resources/db/migration/`에 두었습니다.

## 관련 문서

- [기획서 §8.2, §10, §12](../../../../../../StoryForge_기획서_v1.2.md)
