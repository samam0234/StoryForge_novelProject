# 포트 인벤토리

기준값은 [../plan/CURRENT_STACK.md](../plan/CURRENT_STACK.md)입니다. 여기는 환경별 실제 배치를 적습니다. 시크릿은 적지 않습니다.

## 로컬 개발

| 서비스 | 바인딩 | 외부 노출 | 비고 |
|--------|--------|-----------|------|
| storyforge-console | `localhost:4201` | 개발자 PC의 운영자 | 독립 Angular. `/api`를 8080으로 프록시 |
| storyforge-frontend | `localhost:4200` | 개발자 PC | `ng serve`. `/api`를 8080으로 프록시 |
| storyforge-backend | `0.0.0.0:8080` | 개발자 PC | 외부 API 단일 표면 |
| storyforge-ai | `127.0.0.1:8001` | **없음** | 내부 전용. 인증이 없으므로 외부에 열지 않습니다 |
| PostgreSQL | `0.0.0.0:5433` → 컨테이너 5432 | 개발자 PC | 운영에서는 외부에 열지 않습니다 |

같은 PC의 다른 프로젝트와 겹쳐 AI는 8000 대신 8001, DB는 5432 대신 5433을 씁니다. [../repeater/local-port-conflicts.md](../repeater/local-port-conflicts.md)

## 배포 구성

[docker-compose.deploy.yml](../../docker-compose.deploy.yml)이 PostgreSQL, AI, 백엔드, 프론트를 한 네트워크에 올립니다. 개발용 `docker compose up`은 바꾸지 않습니다.

| 서비스 | 호스트 게시 | 비고 |
|--------|-------------|------|
| frontend | `${FRONTEND_PORT:-80}` | nginx가 `/api`와 웹소켓을 backend로 넘김 |
| backend | `${BACKEND_PORT:-8080}` | `JWT_SECRET`, `FRONTEND_ORIGIN`, `CONSOLE_ORIGIN` 필수 |
| ai | 없음 | 컨테이너 안에서만 8001. 브라우저는 이 주소로 접속하지 않음 |
| postgres | 없음 | 컨테이너 안에서만 5432 |

운영 콘솔은 이 구성에 포함하지 않습니다. 콘솔 출처는 `CONSOLE_ORIGIN`으로 CORS에만 넣습니다. 도메인과 TLS는 이 파일 앞단의 프록시에서 정합니다. 시크릿은 이미지에 넣지 않습니다.
