# feat(api): Spring Boot 기초 API와 파이프라인 상태 머신 구현

## 제목 / 커밋번호

- **제목**: `feat(api): Spring Boot 기초 API와 파이프라인 상태 머신 구현`
- **커밋번호**: `5349f0a`
- **브랜치**: `feature/web-foundation`
- **일시**: 2026-10-22 03:44

## 주 내용

Spring Boot 3.5.16으로 프로젝트·Bible·Outline·챕터 API, 파이프라인 상태 머신, FastAPI 연동, 챕터 SSE 중계를 만들었습니다.

## 상세

- 기획서 §12의 3.x를 따라 3.5.16 사용 (start.spring.io는 4.x만 제공해 pom 직접 작성, Maven Wrapper만 가져옴)
- Flyway `V1__init_schema.sql`: 기획서 §10 엔티티 6개
- `PipelineStateMachine`: 기획서 전이(DEFINED)와 임시 전이(PROVISIONAL) 분리. 임시 전이는 사용자 확인 대기
- `PipelineOrchestrator`: 짧은 트랜잭션 + 트랜잭션 밖 AI 호출, 실패·연결 종료 시 상태 복귀
- §11 목록 밖 보조 엔드포인트 추가: bible/outline confirm, 조회·수정 API
- `docker-compose.yml`(PostgreSQL 16, 5433), `.env.example`, `.gitattributes`(LF 고정)
- OutlineService는 기획서 §8.2에 service/outline이 없어 service/project에 둠

## 결과

- `./mvnw test` 통과 (상태 머신 테스트 3건)
- 실제 실행: 단계 건너뛰기 409, AI 중단 시 502·SSE error 후 상태 복귀, 2챕터 프로젝트를 COMPLETED까지 진행 확인
- 첫 실행에서 리포지토리 0개 인식 오류가 한 번 났으나 재실행에서 재현되지 않음 (원인 미확인)
- 남은 작업: JWT 인증(현재 개방), Rate Limiting, Circuit Breaker
