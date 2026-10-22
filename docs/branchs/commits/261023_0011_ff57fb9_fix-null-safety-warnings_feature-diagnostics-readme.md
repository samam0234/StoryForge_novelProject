# fix(api): VS Code null 안전성 경고와 Spring 힌트 정리

## 제목 / 커밋번호

- **제목**: `fix(api): VS Code null 안전성 경고와 Spring 힌트 정리`
- **커밋번호**: `ff57fb9`
- **브랜치**: `feature/diagnostics-readme`
- **일시**: 2026-10-23 00:11

## 주 내용

VS Code Problems 패널에 뜨던 백엔드 Java 경고(null 안전성, Spring 힌트)를 정리했습니다.

## 상세

- `PipelineOrchestrator`: `TransactionTemplate.execute`의 nullable 결과를 `inTx()` 헬퍼로 검사 — `prepared` null 가능성 경고는 실제 결함 가능성이었음
- `PipelineOrchestrator`: SSE `event()`/`data()`를 한 번만 읽어 지역 변수로 처리, 쓰지 않는 import 삭제
- `AiServiceClient`, `WebClientConfig`, `ProjectService`, `ChapterService`: Spring API 호출 지점에 `Objects.requireNonNull`
- `config/*`: `@Bean` 메서드 `public` 제거 (vscode-spring-boot 힌트)
- 패키지 단위 `@NonNullApi`는 시험 후 경고가 늘어 되돌림
- 유지: `pom.xml` Spring Boot 3.5 OSS 지원 종료 경고(기획서 §12 3.x — 사용자 결정 대기), `SecurityConfig` TODO(auth) 알림

## 결과

- VS Code 진단: Java 경고 0건 (위 2건 제외)
- `./mvnw test` 통과
- 전체 스택 재실행: 2챕터 프로젝트 COMPLETED까지 정상, AI 중단 시 error 이벤트 후 OUTLINE_CONFIRMED로 복귀 확인
- 프론트 `ng build`·`tsc`(spec), AI `pyright` 모두 0건
