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

모두 기본값이 있어서 로컬에서는 설정 없이 실행됩니다. 기준값은 [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)에 있습니다.

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BACKEND_PORT` | `8080` | 서버 포트 |
| `DB_URL` | `jdbc:postgresql://localhost:5433/storyforge` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `storyforge` / `storyforge` | 개발용. 운영에서는 반드시 바꿉니다 |
| `AI_SERVICE_URL` | `http://localhost:8001` | FastAPI 주소 (내부망) |
| `FRONTEND_ORIGIN` | `http://localhost:4200` | CORS 허용 출처 (쉼표로 여러 개) |
| `JWT_SECRET` | (없음) | 32바이트 이상. 비우면 시작할 때 무작위로 만들고 경고를 남깁니다 (재시작하면 로그인 풀림). **운영에서는 필수** |
| `JWT_TTL` | `12h` | 토큰 유효 기간 |
| `RATE_LIMIT_AUTH_PER_MINUTE` | `20` | IP당 로그인·가입 요청 |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | `30` | 사용자당 AI 생성 요청 |

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
| 내보내기 | `GET /api/v1/projects/{id}/export?format=md\|docx\|pdf` | 승인된 챕터가 있을 때 |
| 이력 | `GET .../bible/versions`, `GET /api/v1/chapters/{id}/versions` | — |
| 설정 | `PUT /api/v1/projects/{id}` | — |

- 나머지 API는 모두 `Authorization: Bearer <토큰>`이 필요합니다. 없으면 **401**, 다른 사용자의 리소스는 **404**입니다 ([auth.md](../docs/plan/auth.md)).
- 허용되지 않는 상태에서 요청하면 **409**, AI 서비스 장애는 **502**입니다.
- 챕터는 "확정된 챕터 수 + 1"번만 생성할 수 있습니다. 앞 챕터를 승인해야 다음으로 넘어갑니다.
- 상태 전이는 [PipelineStateMachine](src/main/java/com/storyforge/service/orchestration/PipelineStateMachine.java)에서만 일어납니다.

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
    │   ├── domain/               # Entity, DTO, Repository (user, project, bible, outline, chapter)
    │   ├── client/               # FastAPI WebClient + 내부 계약(AiDtos)
    │   ├── config/               # Security, JWT, 소유자 검사, WebClient, OpenAPI
    │   └── exception/            # ProblemDetail 변환
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/         # Flyway
    └── test/                     # 단위 테스트
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
- Bible·Outline 확정 취소 (기획서에 정의 없음)
- Prometheus 지표 (기획서 §12)
- 여러 서버 구성 지원 (Rate Limiting과 상태 복구가 서버 한 대 기준, [stability.md](../docs/plan/stability.md))

## 관련 문서

- [기획서 §7, §8.2, §10, §11](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/](../docs/Architecture/) — 구조, 상태 머신
- [docs/plan/api-v1.md](../docs/plan/api-v1.md), [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
