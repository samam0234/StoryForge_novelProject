# 로컬 포트 충돌 (5432, 8000, 8080)

## 환경

- Windows 11 개발 PC, 서비스 3개를 로컬 프로세스로 실행 (`docs/guidance/local-dev-setup.md`)
- 같은 PC에 다른 프로젝트의 PostgreSQL(5432), Django 개발 서버(8000)가 함께 떠 있을 수 있음

## 증상

| 포트 | 증상 |
|------|------|
| 5432 | 처음 구성 때 로컬 PostgreSQL 서비스가 5432를 사용 중 |
| 8000 | `uvicorn` 시작 실패, 백엔드의 Bible 생성이 **502**. `curl 127.0.0.1:8000/health`가 Django 404 페이지(HTML)를 돌려줌 |
| 8080 | `Port 8080 was already in use`, `Failed to start bean 'webServerStartStop'` |

## 원인

- 5432·8000: 다른 프로젝트의 서버가 같은 포트를 씁니다. 8000의 경우 백엔드 요청이 엉뚱한 서버로 갑니다.
- 8080: `./mvnw spring-boot:run`을 띄운 셸만 종료하면 Maven이 띄운 **자식 JVM(`StoryforgeApplication`)이 남아** 포트를 계속 잡고 있습니다.

## 해결

1. 포트를 쓰는 프로세스를 확인합니다.
   ```
   netstat -ano | findstr LISTENING | findstr ":8000"
   powershell "(Get-CimInstance Win32_Process -Filter 'ProcessId=<PID>').CommandLine"
   ```
2. **StoryForge 프로세스면** 종료합니다 (`taskkill /PID <PID> /F`). 다른 프로젝트 프로세스면 건드리지 않습니다.
3. 다른 프로젝트가 포트를 쓰고 있으면 StoryForge 쪽 포트를 바꿉니다.
   - DB: 이미 `DB_PORT=5433`으로 피했습니다.
   - AI: `--port 8001`로 실행하고, 백엔드를 `AI_SERVICE_URL=http://localhost:8001`로 실행합니다.
   - 백엔드: `BACKEND_PORT`를 바꾸고 `storyforge-frontend/proxy.conf.json`의 `target`도 맞춥니다.
4. 포트를 계속 바꿔 쓸 거라면 `.env`에 적고, 기준값 변경이면 `docs/plan/CURRENT_STACK.md`를 먼저 고칩니다.

## 검증 명령

```
curl http://127.0.0.1:8001/health            # {"status":"ok","provider":"mock",...} — JSON이어야 함 (HTML이면 다른 서버)
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

## 최초 발생

- 5432: 2026-10-22 (초기 구성)
- 8000, 8080: 2026-10-23 — [../find_debug/2026-10-23.md](../find_debug/2026-10-23.md)
