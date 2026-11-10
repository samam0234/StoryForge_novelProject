# storyforge-backend/

StoryForge의 **Spring Boot 4 Orchestrator**입니다. 외부에 노출되는 단일 API(`/api/v1`)이며, 파이프라인 상태 머신과 데이터 저장을 맡습니다.
AI가 필요한 단계만 내부망의 FastAPI(`storyforge-ai`)에 맡깁니다.

---

## 실행 방법

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| JDK | 21 이상 | 소스·타깃 21. 현재 개발 PC는 JDK 26으로 실행 중 (아래 참고) |
| Docker | 실행 중 | PostgreSQL 16 컨테이너 |
| Maven | 설치 불필요 | Maven Wrapper(`mvnw`)가 Maven 3.10을 자동으로 내려받습니다 |
| AI 서비스 | `http://localhost:8001` | 없어도 서버는 뜹니다. 생성 요청만 502로 실패합니다 |

### 1. DB 실행 (저장소 루트에서)

```bash
docker compose up -d
docker compose ps        # storyforge-postgres가 healthy인지 확인
```

PostgreSQL은 호스트 **5433** 포트를 씁니다. 로컬에 5432를 쓰는 PostgreSQL이 이미 있어서 겹치지 않게 했습니다.

### 2. 서버 실행

```bash
cd storyforge-backend
./mvnw spring-boot:run          # Windows PowerShell/cmd: .\mvnw.cmd spring-boot:run
```

- 주소: http://localhost:8080
- 시작할 때 Flyway가 [db/migration](src/main/resources/db/migration/)의 스키마를 자동으로 적용합니다.
- `Started StoryforgeApplication` 로그가 나오면 준비된 것입니다.

### 3. 확인

| 확인 | 명령 / 주소 | 기대 결과 |
|------|-------------|-----------|
| 상태 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger UI |
| 인증 확인 | `curl -i http://localhost:8080/api/v1/projects` | 토큰이 없으면 `401` |

### 명령어

| 하려는 일 | 명령 | 결과 |
|-----------|------|------|
| 개발 실행 | `./mvnw spring-boot:run` | 8080 |
| 테스트 | `./mvnw test` | 단위 + 통합 테스트. 통합 테스트는 **Docker로 PostgreSQL을 띄웁니다** (Docker가 꺼져 있으면 건너뜀) |
| JAR 빌드 | `./mvnw package` | `target/storyforge-backend-0.0.1-SNAPSHOT.jar` |
| JAR 실행 | `java -jar target/storyforge-backend-0.0.1-SNAPSHOT.jar` | 8080 |
| DB 초기화 | 루트에서 `docker compose down -v && docker compose up -d` | 데이터 삭제 후 재시작 시 스키마 재생성 |

### 환경 변수

로컬 개발 기본값은 설정 없이 실행됩니다. prod에서는 DB·JWT와 웹·콘솔의 HTTPS 출처를 명시해야 합니다. 기준값은 [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)에 있습니다.

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BACKEND_PORT` | `8080` | 서버 포트 |
| `DB_URL` | `jdbc:postgresql://localhost:5433/storyforge` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `storyforge` / `storyforge` | 개발용. 운영에서는 반드시 바꿉니다 |
| `AI_SERVICE_URL` | `http://localhost:8001` | FastAPI 주소 (내부망) |
| `CONSOLE_ORIGIN` | `http://localhost:4201` | 독립 운영 콘솔 출처 |
| `FRONTEND_ORIGIN` | `http://localhost:4200` | 개발 CORS 출처. prod는 정확한 HTTPS 출처 하나 |
| `JWT_SECRET` | (없음) | 32바이트 이상. 비우면 시작할 때 무작위로 만들고 경고를 남깁니다 (재시작하면 로그인 풀림). **운영에서는 필수** |
| `JWT_TTL` | `12h` | 토큰 유효 기간 |
| `RATE_LIMIT_AUTH_PER_MINUTE` | `20` | IP당 로그인·가입 요청 |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | `30` | 사용자당 AI 호출 (생성, 가이드챗, 패키지 검사) |
| `RATE_LIMIT_COMMUNITY_PER_MINUTE` | `60` | 사용자당 글·댓글·공유·Clone·신고, IP당 공유 방문 |
| `ADMIN_EMAILS` | (없음) | 관리자 이메일. 쉼표로 구분 ([moderation.md](../docs/plan/moderation.md)) |
| `TERMS_VERSION` | `2026-11-04` | 이용약관 버전 |
| `COMMUNITY_ZONE` | `Asia/Seoul` | 작업 그래프와 활동 잔디의 날짜 기준 |
| `CODICOMPERCHAT_URL` | (없음) | 가이드챗 콜라보 이동 주소 |

예: `DB_PASSWORD=secret BACKEND_PORT=9090 ./mvnw spring-boot:run`

---

## API 요약

전체 목록과 형식은 [docs/plan/api-v1.md](../docs/plan/api-v1.md)에 있습니다. 오류는 모두 ProblemDetail JSON(`detail`에 사용자 메시지)으로 돌려줍니다.

| 단계 | 요청 | 허용 상태 |
|------|------|-----------|
| 가입 / 로그인 | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | 토큰 불필요 |
| 프로젝트 생성 | `POST /api/v1/projects` | — |
| Bible 생성 / 저장 / 확정 | `POST .../bible/generate`, `PUT .../bible`, `PUT .../bible/confirm` | `BIBLE_DRAFT` |
| Outline 생성 / 저장 / 확정 | `POST .../outline/generate`, `PUT .../outline`, `PUT .../outline/confirm` | `BIBLE_CONFIRMED`, `OUTLINE_DRAFT` |
| 챕터 생성 (SSE) | `POST .../chapters/{num}/generate` | `OUTLINE_CONFIRMED`, `CHAPTER_REVIEW` |
| 챕터 승인 | `PUT /api/v1/chapters/{id}/confirm` | `CHAPTER_REVIEW` |
| 내보내기 | `GET /api/v1/projects/{id}/export?format=md\|txt\|docx\|pdf\|html\|epub\|odt\|rtf` | 승인된 챕터가 있을 때 |
| 게시 | `POST /api/v1/projects/{id}/publish` | 승인된 챕터가 있을 때. 상태는 바뀌지 않음 |
| 이력 | `GET .../bible/versions`, `GET /api/v1/chapters/{id}/versions` | — |
| 설정 | `PUT /api/v1/projects/{id}` | — |

- 커뮤니티 API(게시판, Clone, 공유, 패키지, 라운지, 신고, 알림)는 [api-v1.md](../docs/plan/api-v1.md)에 있습니다. 읽기만 로그인 없이 됩니다.
- 나머지 API는 모두 `Authorization: Bearer <토큰>`이 필요합니다. 없으면 **401**, 다른 사용자의 리소스는 **404**, 정지된 계정의 쓰기는 **403**입니다 ([auth.md](../docs/plan/auth.md)).
- 고친 챕터는 승인할 때 AI로 다시 검증합니다. AI가 꺼져 있으면 승인이 **502**입니다.
- 허용되지 않는 상태에서 요청하면 **409**, AI 서비스 장애는 **502**입니다.
- 챕터는 "확정된 챕터 수 + 1"번만 생성할 수 있습니다. 앞 챕터를 승인해야 다음으로 넘어갑니다.
- 상태 전이는 [PipelineStateMachine](src/main/java/com/storyforge/service/orchestration/PipelineStateMachine.java)에서만 일어납니다.

Spring Boot는 `.env`를 자동으로 읽지 않습니다. `ADMIN_EMAILS`, `JWT_SECRET`, DB 변수 등은 백엔드를 실행하는 셸이나 배포 도구로 주입합니다.

## 운영 API

`ConsoleController`는 운영 집계·회원 검색과 정지·신고 일괄 조치·감사 조회를 제공합니다. 관리자 권한은 서비스에서 확인합니다. V11 운영 잠금을 사용하는 조치와 알림·감사 기록은 같은 트랜잭션입니다. [스펙](../docs/plan/operations-console.md).

## 폴더 구조

```
storyforge-backend/
├── pom.xml                       # Spring Boot 4.1.2, springdoc
├── mvnw, mvnw.cmd, .mvn/         # Maven Wrapper
└── src/
    ├── main/java/com/storyforge/
    │   ├── controller/           # REST (/api/v1)
    │   ├── service/
    │   │   ├── orchestration/    # 상태 머신, 파이프라인 실행
    │   │   ├── auth/ project/ bible/ chapter/
    │   │   ├── community/        # 게시, Clone, 공유, 패키지, 라운지, 활동, 업적, 이슈, 가이드챗
    │   │   ├── moderation/ notification/   # 신고·관리, 알림
    │   ├── domain/               # Entity, DTO, Repository (user, project, bible, outline, chapter, community, activity, moderation, notification)
    │   ├── client/               # FastAPI WebClient + 내부 계약(AiDtos)
    │   ├── config/               # Security, JWT, 주인·공동 창작자 검사, WebClient, OpenAPI
    │   └── exception/            # ProblemDetail 변환
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/         # Flyway
    └── test/                     # 단위·통합 테스트 97건 (실제 PostgreSQL·로컬 SMTP)
```

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| `Connection to localhost:5433 refused` | DB가 꺼져 있습니다. 루트에서 `docker compose up -d` 후 `docker compose ps`로 healthy 확인 |
| `Port 8080 was already in use` | 이전에 띄운 백엔드 JVM이 남아 있는 경우가 많습니다 (셸만 닫으면 JVM이 남음). [local-port-conflicts.md](../docs/repeater/local-port-conflicts.md)의 확인 명령으로 PID를 찾아 종료하거나 `BACKEND_PORT`를 바꿉니다 |
| 생성 요청이 502 | AI 서비스(8001)가 꺼져 있습니다. `storyforge-ai`를 실행합니다. 실패가 반복되면 Circuit Breaker가 30초간 호출을 막습니다 |
| 429 응답 | 요청 한도 초과입니다 ([stability.md](../docs/plan/stability.md)). 1분 뒤 풀립니다 |
| 시작 로그에 `sun.misc.Unsafe` 경고 | JDK 26에서 Netty가 내는 경고로, 동작에는 영향이 없습니다 |

## 아직 없는 것

- 리프레시 토큰 (토큰이 만료되면 다시 로그인)
- SMTP 실제 공급자·발신 주소 확정과 외부 수신 검증 ([로컬 검증 완료](../docs/guidance/account-mail.md))
- Bible·Outline 확정 취소 (기획서에 정의 없음)
- Prometheus 지표 (기획서 §12)
- 여러 서버 구성 지원 (Rate Limiting과 상태 복구가 서버 한 대 기준, [stability.md](../docs/plan/stability.md))

## 관련 문서

- [기획서 §7, §8.2, §10, §11](../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/Architecture/](../docs/Architecture/) — 구조, 상태 머신
- [docs/plan/api-v1.md](../docs/plan/api-v1.md), [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
- 보안: [../SECURITY.md](../SECURITY.md), 라이선스: [../LICENSE](../LICENSE)
