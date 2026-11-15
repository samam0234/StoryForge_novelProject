# storyforge-backend

StoryForge의 **오케스트레이터**입니다. 브라우저가 만나는 유일한 API(`/api/v1`)이며, 인증과 권한, 창작 파이프라인의 상태 머신, 데이터 저장을 맡습니다. AI가 필요한 단계만 내부망의 FastAPI([storyforge-ai](../storyforge-ai/README.md))에 맡기고 그 결과를 중계합니다.

| | |
|---|---|
| **기술** | Spring Boot 4.1.2 (Java 21), Spring Security(JWT), WebSocket, JPA(Hibernate 7), Flyway, Resilience4j, springdoc-openapi |
| **주소** | 개발 http://localhost:8080 |
| **데이터** | PostgreSQL 16, Flyway 마이그레이션 V1~V15 |
| **호출하는 곳** | FastAPI `http://localhost:8001` (내부 HTTP·SSE·WebSocket), SMTP(선택) |
| **테스트** | 108건. 통합 테스트는 실제 PostgreSQL과 로컬 SMTP 사용 |

핵심 원칙은 하나입니다. **무엇이 확정됐는지는 이 서비스만 결정합니다.** 프로젝트 상태를 바꾸는 코드는 [PipelineStateMachine](src/main/java/com/storyforge/service/orchestration/PipelineStateMachine.java) 한 곳뿐이고, AI 서비스의 결과는 사용자가 승인하기 전까지 Draft입니다.

## 목차

- [실행](#실행)
- [환경 변수](#환경-변수)
- [요청이 지나는 길](#요청이-지나는-길)
- [파이프라인과 상태 머신](#파이프라인과-상태-머신)
- [API 요약](#api-요약)
- [AI 서비스와의 연결](#ai-서비스와의-연결)
- [계정과 권한](#계정과-권한)
- [데이터베이스](#데이터베이스)
- [구조](#구조)
- [테스트](#테스트)
- [운영 프로필과 배포](#운영-프로필과-배포)
- [보조 도구](#보조-도구)
- [문제 해결](#문제-해결)
- [한계](#한계)

---

## 실행

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| JDK | 21 이상 | 소스·타깃 21. 개발 PC는 JDK 26으로 실행 중 |
| Docker | 실행 중 | PostgreSQL 16 컨테이너, 통합 테스트 |
| Maven | 설치 불필요 | Maven Wrapper(`mvnw`)가 자동으로 내려받습니다 |
| AI 서비스 | `http://localhost:8001` | 없어도 서버는 뜹니다. 생성 요청만 502로 실패합니다 |

### 1. DB (저장소 루트에서)

```bash
docker compose up -d
docker compose ps        # storyforge-postgres가 healthy인지 확인
```

PostgreSQL은 호스트 **5433** 포트를 씁니다. 로컬에 5432를 쓰는 PostgreSQL이 이미 있어 겹치지 않게 했습니다.

### 2. 서버

```bash
cd storyforge-backend
./mvnw spring-boot:run          # Windows PowerShell/cmd: .\mvnw.cmd spring-boot:run
```

- 시작할 때 Flyway가 [db/migration](src/main/resources/db/migration/)의 스키마를 자동으로 적용합니다.
- `Started StoryforgeApplication` 로그가 나오면 준비된 것입니다.
- **Spring Boot는 `.env`를 자동으로 읽지 않습니다.** 환경 변수는 실행 셸에서 주입합니다. Windows에서 루트 `.env`를 읽어 실행하려면 저장소 루트에서 `powershell -File storyforge-backend/scripts/start-local.ps1`을 씁니다.

### 3. 확인

| 확인 | 명령 / 주소 | 기대 결과 |
|------|-------------|-----------|
| 상태 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger UI (운영 프로필에서는 꺼짐) |
| 인증 확인 | `curl -i http://localhost:8080/api/v1/projects` | 토큰이 없으면 `401` |

### 명령어

| 하려는 일 | 명령 | 결과 |
|-----------|------|------|
| 개발 실행 | `./mvnw spring-boot:run` | 8080 |
| 테스트 | `./mvnw test` | 단위 + 통합. Docker가 꺼져 있으면 통합 테스트는 **건너뜀** |
| 테스트 + 패키징 | `./mvnw verify` | `target/storyforge-backend-0.0.1-SNAPSHOT.jar` |
| JAR 실행 | `java -jar target/storyforge-backend-0.0.1-SNAPSHOT.jar` | 8080 |
| DB 초기화 | 루트에서 `docker compose down -v && docker compose up -d` | 데이터 삭제 후 스키마 재생성 |

---

## 환경 변수

로컬 개발은 설정 없이 기본값으로 실행됩니다. 기준값과 전체 설명은 [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)가 원본입니다.

### 기본

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BACKEND_PORT` | `8080` | 서버 포트 |
| `DB_URL` | `jdbc:postgresql://localhost:5433/storyforge` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `storyforge` / `storyforge` | 개발용. 운영에서는 반드시 바꿉니다 |
| `AI_SERVICE_URL` | `http://localhost:8001` | FastAPI 주소 (내부망) |
| `LLM_PROVIDER` | `mock` | AI 서비스와 같은 값이어야 외부 전송 안내가 맞습니다 |
| `FRONTEND_ORIGIN` | `http://localhost:4200` | 사용자 웹 CORS 출처. 메일 링크의 주소로도 씁니다 |
| `CONSOLE_ORIGIN` | `http://localhost:4201` | 운영 콘솔 CORS 출처 |

### 인증과 계정

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `JWT_SECRET` | (없음) | 32바이트 이상. 비우면 시작할 때 무작위로 만들고 경고를 남깁니다 (재시작하면 로그인 풀림). **운영 필수** |
| `JWT_TTL` | `12h` | 액세스 토큰 유효 기간 |
| `JWT_REFRESH_TTL` | `14d` | 갱신 토큰 유효 기간 |
| `ADMIN_EMAILS` | (없음) | 관리자 이메일. 쉼표로 구분 |
| `TERMS_VERSION` | `2026-11-04` | 이용약관 버전. 올리면 기존 사용자는 다시 동의해야 게시 가능 |

### 메일 (기본 비활성)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MAIL_ENABLED` | `false` | 이메일 인증과 비밀번호 재설정 |
| `MAIL_FROM` | (없음) | 켤 때 필수인 발신 주소 |
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` | 로컬 수신기 기준 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | (없음) | 자격 증명. 커밋하지 않습니다 |
| `SMTP_AUTH` / `SMTP_STARTTLS` / `SMTP_SSL` | 모두 `false` | 외부 공급자는 인증과 TLS 설정이 필요합니다 |

### 보호와 운영

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `RATE_LIMIT_AUTH_PER_MINUTE` | `20` | IP당 로그인·가입 |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | `30` | 사용자당 AI 호출 (생성, 가이드챗, 패키지 검사) |
| `RATE_LIMIT_COMMUNITY_PER_MINUTE` | `60` | 사용자당 글·댓글·공유·Clone·신고, IP당 공유 방문 |
| `TRUSTED_PROXIES` | (없음) | 이 주소에서 온 요청만 `X-Forwarded-For`를 믿습니다 |
| `CONTENT_BLOCKLIST` | (없음) | 쉼표로 구분한 금지 표현. 비우면 검사하지 않습니다 |
| `METRICS_TOKEN` | (없음) | 값이 있을 때만 `X-Metrics-Token`으로 `/actuator/prometheus`. 없으면 404 |
| `COMMUNITY_ZONE` | `Asia/Seoul` | 작업 그래프와 활동 잔디의 날짜 기준 |
| `CODICOMPERCHAT_URL` | (없음) | 가이드챗 콜라보 이동 주소. http(s)가 아니면 버튼을 만들지 않습니다 |

예: `DB_PASSWORD=secret BACKEND_PORT=9090 ./mvnw spring-boot:run`

---

## 요청이 지나는 길

```
요청
 → RateLimitFilter           요청 제한. 넘으면 429 + Retry-After
 → Spring Security            JWT 검증. 없거나 만료면 401
 → AccountGuardInterceptor    쓰기 요청마다 정지·삭제 계정 확인. 정지 403
 → OwnershipInterceptor       경로의 {projectId}, {chapterId} 접근 권한. 남의 것은 404
 → Controller → Service       비즈니스 규칙, 트랜잭션
 → GlobalExceptionHandler     오류를 ProblemDetail JSON으로
```

| 응답 | 뜻 |
|------|----|
| **401** | 토큰이 없거나 만료됐거나, 비밀번호 변경으로 폐기된 토큰 |
| **403** | 정지된 계정의 쓰기, 관리자 전용 API |
| **404** | 없는 리소스, **또는 다른 사용자의 리소스** (존재를 알려 주지 않습니다) |
| **409** | 지금 상태에서 할 수 없는 요청, 동시 수정 충돌, 재검증에서 나온 심각한 이슈 |
| **429** | 요청 한도 초과 |
| **502** | AI 서비스 장애 또는 Circuit Breaker 차단 |
| **503** | 메일 발송 실패 (그 요청만) |

오류 본문은 모두 ProblemDetail이고 `detail`에 사용자에게 보여 줄 메시지가 들어 있습니다.

## 파이프라인과 상태 머신

```
BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED
→ CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED

추가 전이: CHAPTER_REVIEW → CHAPTER_GENERATING        (다음 챕터, 재생성)
           CHAPTER_GENERATING → OUTLINE_CONFIRMED      (첫 챕터 실패·중단 복귀)
```

- 챕터는 **"확정된 챕터 수 + 1"번만** 생성할 수 있습니다. 앞 챕터를 승인해야 다음으로 넘어갑니다.
- 챕터를 생성할 때 Bible, 해당 장의 Outline, 승인된 이전 챕터의 요약, 창작 요구, 직전 장의 마지막 장면을 컨텍스트로 조립해 AI 서비스에 보냅니다.
- AI 호출 동안 DB 트랜잭션을 잡지 않습니다. 상태 확인·컨텍스트 조립 → AI 호출 → 저장·전이를 각각 짧은 트랜잭션으로 나눕니다.
- AI 스트림이 `done` 없이 끝나면 실패입니다. 부분 원고를 저장하지 않고 생성 전 상태로 되돌립니다.
- 사람이 고친 챕터는 승인할 때 요약과 Continuity를 다시 만듭니다. 심각한 이슈가 나오면 저장한 뒤 **409로 한 번 멈추고**, 다시 승인하면 진행합니다. AI가 실패하면 승인하지 않습니다(502).
- 재검증 중에 본문이 바뀌면 낙관적 잠금으로 감지해 409로 거부합니다.
- `CHAPTER_GENERATING`에 210초보다 오래 머문 프로젝트는 시작할 때와 1분마다 되돌립니다.

상세 규칙은 [pipeline-state-machine.md](../docs/Architecture/pipeline-state-machine.md)와 [orchestration/README.md](src/main/java/com/storyforge/service/orchestration/README.md)에 있습니다.

## API 요약

전체 목록과 요청·응답 형식은 [api-v1.md](../docs/plan/api-v1.md)에 있습니다. 여기는 창작 흐름의 뼈대만 적습니다.

| 단계 | 요청 | 허용 상태 |
|------|------|-----------|
| 가입 / 로그인 / 갱신 | `POST /api/v1/auth/register`, `/login`, `/refresh` | 토큰 불필요 |
| 프로젝트 생성 | `POST /api/v1/projects` | — |
| Bible 생성 / 저장 / 확정 | `POST .../bible/generate`, `PUT .../bible`, `PUT .../bible/confirm` | `BIBLE_DRAFT` |
| Outline 생성 / 저장 / 확정 | `POST .../outline/generate`, `PUT .../outline`, `PUT .../outline/confirm` | `BIBLE_CONFIRMED`, `OUTLINE_DRAFT` |
| 챕터 생성 (SSE) | `POST .../chapters/{num}/generate` | `OUTLINE_CONFIRMED`, `CHAPTER_REVIEW` |
| 챕터 수정 / 승인 | `PUT /api/v1/chapters/{id}`, `PUT /api/v1/chapters/{id}/confirm` | `CHAPTER_REVIEW` |
| 가이드챗·장면 개척 | `GET /api/v1/ws?access_token=` (WebSocket) | 로그인 필요 |
| 내보내기 | `GET /api/v1/projects/{id}/export?format=md\|txt\|docx\|pdf\|html\|epub\|odt\|rtf` | 승인된 챕터가 있을 때 |
| 게시 | `POST /api/v1/projects/{id}/publish` | 승인된 챕터가 있을 때. 상태는 바뀌지 않음 |
| 이력 | `GET .../bible/versions`, `GET /api/v1/chapters/{id}/versions` | — |

그 밖의 묶음은 다음과 같습니다.

| 묶음 | 컨트롤러 | 스펙 |
|------|----------|------|
| 게시판, 작품, Clone, 공유, 추천, 댓글 | `WorkController`, `ProjectCommunityController` | [community.md](../docs/plan/community.md) |
| 패키지포지, 전체 Continuity 검사 | `PackageController` | 〃 |
| 라운지, 이슈, 프로필, 계정 | `PostController`, `UserController` | 〃 |
| 함께 창작 (초대, 작업 커밋) | `CollaborationController` | 〃 |
| 표지 이미지 | `MediaController` | 〃 |
| AI 가이드챗 (REST) | `GuideController` | 〃 |
| 신고, 차단, 관리자 처리 | `ModerationController` | [moderation.md](../docs/plan/moderation.md) |
| 알림 | `NotificationController` | 〃 |
| 이메일 인증, 비밀번호 재설정 | `AccountMailController` | [auth.md](../docs/plan/auth.md) |
| 운영 집계, 회원 조치, 일괄 조치, 감사 | `ConsoleController` | [operations-console.md](../docs/plan/operations-console.md) |

커뮤니티의 **읽기** API만 로그인 없이 열려 있습니다. 공유 링크를 바로 열 수 있어야 하기 때문입니다.

## AI 서비스와의 연결

| 용도 | 방식 | 구현 |
|------|------|------|
| Bible, Outline, 재검증, 패키지 검사, 가이드챗 | 내부 HTTP (동기) | `client/AiServiceClient` |
| 챕터 본문 | 내부 SSE를 받아 브라우저로 그대로 중계 | `client/AiServiceClient`, `PipelineOrchestrator` |
| 가이드챗, 장면 개척 (실시간) | 브라우저 소켓이 열린 동안 FastAPI `/internal/ws` 연결 하나 유지 | `client/AiLiveClient`, `config/LiveSessionHandler` |

장애에 대비한 장치는 [stability.md](../docs/plan/stability.md)에 있습니다.

- **타임아웃:** 연결 5초, 응답 180초.
- **재시도:** 동기 호출은 연결 실패 시 한 번.
- **Circuit Breaker:** 최근 10회 중 절반 이상 실패하면(최소 5회) 30초 동안 호출을 막고 바로 502를 돌려줍니다.
- 내부 API의 JSON 계약은 [AiDtos.java](src/main/java/com/storyforge/client/AiDtos.java)와 AI 서비스의 `schemas.py`가 짝입니다. 한쪽을 바꾸면 **같은 커밋에서** 다른 쪽도 바꿉니다.

## 계정과 권한

- **토큰:** HS256 JWT 액세스 토큰과 갱신 토큰. 비밀번호를 바꾸거나 재설정하면 자격 증명 버전이 올라 이전 토큰이 모두 거부됩니다.
- **소유권:** `OwnershipInterceptor`가 경로 변수로 일괄 검사합니다. 주인과 초대를 수락한 공동 창작자만 통과합니다. 삭제·설정·게시·초대는 서비스가 주인인지 다시 확인합니다.
- **관리자:** DB 역할이 아니라 `ADMIN_EMAILS` 설정입니다. 화면이나 API로 권한을 올릴 길을 두지 않기 위해서입니다. 메일을 켠 환경에서는 이메일 인증까지 끝나야 관리자가 됩니다.
- **정지:** 토큰은 만료 전까지 유효하므로 쓰기 요청마다 DB에서 계정 상태를 확인합니다. 읽기는 막지 않습니다.
- **메일:** 인증과 재설정 링크는 일회용이고 해시로 저장합니다. 같은 목적의 발송은 60초 간격입니다. SMTP 장애는 그 요청만 503이며 `/actuator/health`를 내리지 않습니다.
- **운영 조치:** 단건·일괄·회원 직접 조치가 같은 DB 잠금 행을 잡아 직렬화됩니다. 변경, 알림, 감사 기록은 한 트랜잭션이라 일부만 반영되지 않습니다.

## 데이터베이스

스키마는 Flyway가 관리합니다. Hibernate는 `ddl-auto: validate`로 검증만 합니다.

| 버전 | 내용 |
|------|------|
| V1 | 기획서 §10 기본 테이블 6개 |
| V2 ~ V4 | 사용자와 소유자, 프로젝트 낙관적 락, 프로젝트 설정과 Bible·챕터 이력 |
| V5 ~ V6 | 커뮤니티(작품, Clone, 공유, 패키지, 글, 댓글, 활동, 업적), 함께 창작 |
| V7 | 승인 전 재검증 표시 |
| V8 ~ V9 | 신고, 차단, 알림, 정지, 약관 동의, 숨김, 패키지 검사 결과 |
| V10 ~ V11 | 표지, 운영 조치 잠금과 감사 기록 |
| V12 ~ V13 | 메일 토큰과 자격 증명 버전, 챕터 낙관적 잠금 |
| V14 ~ V15 | 최초 창작 요구 저장, 갱신 토큰, 여러 서버가 함께 쓰는 요청 제한 창 |

**이미 적용된 마이그레이션은 고치지 않습니다.** 바꿀 것이 있으면 새 버전 파일을 추가합니다. 파일별 설명은 [db/migration/README.md](src/main/resources/db/migration/README.md)에 있습니다.

## 구조

```
storyforge-backend/
├── pom.xml                       # Spring Boot 4.1.2
├── mvnw, mvnw.cmd, .mvn/         # Maven Wrapper
├── Dockerfile                    # 배포 이미지 (JDK 21 빌드 → JRE 21 실행)
├── scripts/                      # 수동 검증·로컬 실행 도구
└── src/
    ├── main/java/com/storyforge/
    │   ├── controller/           # REST (/api/v1). 17개
    │   ├── service/
    │   │   ├── orchestration/    # 상태 머신, 파이프라인 실행, 복구, 장면 개척
    │   │   ├── auth/             # 가입·로그인, 토큰, 갱신, 계정 메일, 관리자 판별
    │   │   ├── project/ bible/ chapter/   # 프로젝트·Outline·내보내기·함께 창작, Bible, 챕터
    │   │   ├── community/        # 게시, Clone, 공유, 패키지, 라운지, 활동, 업적, 가이드챗
    │   │   ├── moderation/       # 신고, 관리자 조치, 운영 콘솔, 감사
    │   │   └── notification/     # 알림
    │   ├── domain/               # Entity, Repository, DTO
    │   ├── client/               # FastAPI 클라이언트, 내부 계약
    │   ├── config/               # 보안, JWT, 요청 제한, 권한 검사, 웹소켓, 운영 검사
    │   └── exception/            # 예외 → ProblemDetail
    ├── main/resources/
    │   ├── application.yml       # 기본 설정 (환경 변수 기본값)
    │   ├── application-prod.yml  # 운영 프로필
    │   ├── db/migration/         # Flyway V1~V15
    │   └── fonts/                # PDF 내보내기용 나눔고딕
    └── test/                     # 단위·통합 테스트
```

| 폴더 | 안내 |
|------|------|
| `com/storyforge/` | [README](src/main/java/com/storyforge/README.md) — 컨트롤러, 서비스, 클라이언트, 예외 |
| `service/orchestration/` | [README](src/main/java/com/storyforge/service/orchestration/README.md) — 상태 머신 |
| `config/` | [README](src/main/java/com/storyforge/config/README.md) — 보안과 필터 |
| `domain/` | [README](src/main/java/com/storyforge/domain/README.md) — 엔티티 |
| `db/migration/` | [README](src/main/resources/db/migration/README.md) — 마이그레이션 |
| `test/` | [README](src/test/README.md) — 테스트 목록 |

## 테스트

```bash
./mvnw test
```

- 108건입니다 (2026-11-15). 통합 테스트는 Testcontainers로 **실제 PostgreSQL**을 띄우고, 메일 테스트는 임의 포트의 로컬 SMTP 수신기를 씁니다.
- **Docker가 꺼져 있으면 통합 테스트는 실패가 아니라 건너뜀입니다.** 결과의 `Skipped`가 0인지 꼭 확인하세요.
- AI는 `StubAiServer`(실제 HTTP·SSE로 내부 API를 흉내 냄)로 대신합니다. 실제 모델을 호출하는 테스트는 넣지 않습니다.
- 새 엔드포인트를 추가하면 "다른 사용자는 404" 케이스를 함께 추가합니다.

테스트별 범위는 [src/test/README.md](src/test/README.md)에 있습니다.

## 운영 프로필과 배포

`spring.profiles.active=prod`로 띄우면 [application-prod.yml](src/main/resources/application-prod.yml)이 적용됩니다.

- `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_ORIGIN`, `CONSOLE_ORIGIN`에 **기본값이 없습니다.** 빠지면 시작하지 않습니다.
- 두 출처는 각각 경로·와일드카드·후행 슬래시가 없는 HTTPS 출처 하나여야 합니다. 개발용 DB 자격 증명과 빈 JWT 키는 거부합니다 (`config/ProductionChecks`).
- Swagger UI와 API 문서는 꺼집니다.

[Dockerfile](Dockerfile)은 JDK 21에서 JAR를 만들고 JRE 21에서 실행합니다. 전체 구성은 루트의 [docker-compose.deploy.yml](../docker-compose.deploy.yml)에 있습니다. 실제 서버에 배포해 검증한 적은 없습니다. 운영 전에 [SECURITY.md](../SECURITY.md)의 체크리스트를 확인하세요.

## 보조 도구

수동 검증 도구는 `scripts/`에 있고 AI 서비스의 venv로 실행합니다. 자동 회귀 테스트는 `src/test/`에 둡니다.

| 도구 | 하는 일 |
|------|---------|
| `scripts/start-local.ps1` | 루트 `.env`를 읽어 백엔드를 실행 (Windows) |
| `scripts/check_smtp.py` | SMTP 접속 확인 |
| `scripts/validate_restart.py` | 실제 JVM을 생성 도중 종료하고 재시작해 복구를 확인 |

```powershell
# 먼저 ./mvnw verify 로 JAR를 만든 뒤
../storyforge-ai/.venv/Scripts/python -m scripts.validate_restart --container <로컬-PostgreSQL-컨테이너>
```

`validate_restart`는 무작위 임시 DB·포트와 mock AI로 "첫 장 승인 → 둘째 장 스트리밍 중 JVM 종료 → 재시작 후 첫 장 보존 확인 → 둘째 장 재생성"을 검증합니다. 만든 프로세스와 DB만 정리합니다. 실제 모델이나 브라우저 검증은 아닙니다.

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| `Connection to localhost:5433 refused` | DB가 꺼져 있습니다. 루트에서 `docker compose up -d` 후 `docker compose ps`로 healthy 확인 |
| `Port 8080 was already in use` | 이전 백엔드 JVM이 남은 경우가 많습니다. [local-port-conflicts.md](../docs/repeater/local-port-conflicts.md)로 PID를 찾아 종료하거나 `BACKEND_PORT`를 바꿉니다 |
| 생성 요청이 502 | AI 서비스(8001)가 꺼져 있습니다. 반복되면 Circuit Breaker가 30초간 호출을 막습니다 |
| 승인이 502 | 고친 원고는 재검증이 필요한데 AI가 꺼져 있습니다. 승인되지 않은 상태 그대로입니다 |
| 승인이 409 | 재검증에서 심각한 이슈가 나왔거나, 검증 중 본문이 바뀌었습니다. 리포트를 확인하고 다시 승인합니다 |
| 환경 변수를 넣었는데 적용되지 않음 | Spring Boot는 `.env`를 읽지 않습니다. 실행 셸에 주입했는지, 재시작했는지 확인합니다 |
| 메일 요청이 503 | SMTP 접속이나 인증 실패입니다. `scripts/check_smtp.py`로 확인합니다. 서버 상태는 `UP`으로 유지됩니다 |
| 429 응답 | 요청 한도 초과입니다. 1분 뒤 풀립니다 |
| 테스트가 대부분 Skipped | Docker가 꺼져 있습니다 |
| 시작 로그에 `sun.misc.Unsafe` 경고 | JDK 26에서 Netty가 내는 경고로 동작에는 영향이 없습니다 |

## 한계

- 실제 SMTP 공급자와 발신 주소가 정해지지 않았고 외부 수신함 도달을 확인하지 못했습니다.
- Bible·Outline 확정 취소는 없습니다 (기획서에 정의가 없습니다).
- Codicomperchat은 설정한 주소로 이동하는 것까지만 있습니다. 연동 규격이 필요합니다.
- 이용약관은 법률 검토 전 초안입니다.
- 감사 기록은 수정·삭제 API가 없지만, DB 관리자의 변경까지 막는 불변 저장소는 아닙니다.

## 관련 문서

- [기획서 §7, §8.2, §10, §11](../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/Architecture/](../docs/Architecture/README.md) — 구조, 상태 머신
- [api-v1.md](../docs/plan/api-v1.md), [auth.md](../docs/plan/auth.md), [stability.md](../docs/plan/stability.md), [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- [account-mail.md](../docs/guidance/account-mail.md) — 메일 설정과 로컬 검증
- 저장소 안내 [../README.md](../README.md) · 작업 규칙 [../AGENTS.md](../AGENTS.md) · 보안 [../SECURITY.md](../SECURITY.md) · 라이선스 [../LICENSE](../LICENSE)
