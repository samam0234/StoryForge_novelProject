# 포트 인벤토리

기준값은 [../plan/CURRENT_STACK.md](../plan/CURRENT_STACK.md)입니다. 여기는 환경별 실제 배치를 적습니다. 시크릿은 적지 않습니다.

## 로컬 개발

| 서비스 | 바인딩 | 외부 노출 | 비고 |
|--------|--------|-----------|------|
| storyforge-frontend | `localhost:4200` | 개발자 PC | `ng serve`. `/api`를 8080으로 프록시 |
| storyforge-backend | `0.0.0.0:8080` | 개발자 PC | 외부 API 단일 표면 |
| storyforge-ai | `127.0.0.1:8001` | **없음** | 내부 전용. 인증이 없으므로 외부에 열지 않습니다 |
| PostgreSQL | `0.0.0.0:5433` → 컨테이너 5432 | 개발자 PC | 운영에서는 외부에 열지 않습니다 |

같은 PC의 다른 프로젝트와 겹쳐 AI는 8000 대신 8001, DB는 5432 대신 5433을 씁니다. [../repeater/local-port-conflicts.md](../repeater/local-port-conflicts.md)

## 개발 서버, 운영

아직 없습니다. 배포 구성(Dockerfile, 프록시, 도메인)을 정하면 이 표를 채웁니다.
