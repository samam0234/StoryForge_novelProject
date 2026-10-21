# storyforge-backend/src/main/resources/

Spring Boot **설정 파일과 리소스**를 둡니다.

## 들어갈 것

- `application.yml` (예정) — 값은 환경 변수로 주입하고 기준값은 `docs/plan/CURRENT_STACK.md`
- `db/migration/` — Flyway 마이그레이션

## 들어가지 않을 것

- 시크릿 값 (`.env`, 환경 변수로)

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [db/](db/) | 데이터베이스 리소스 |

## 관련 문서

- [기획서 §8.2](../../../../StoryForge_기획서_v1.2.md)
- [docs/plan/CURRENT_STACK.md](../../../../docs/plan/CURRENT_STACK.md)
- 상위 폴더: [../README.md](../README.md)
