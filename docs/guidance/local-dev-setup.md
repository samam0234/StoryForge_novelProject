# 로컬 개발 환경 실행

## 전제

- Docker Desktop 실행 중
- Node.js 24.14 이상, JDK 21 이상 (현재 개발 PC는 JDK 26), Python 3.10 이상
- 버전 기준은 [../plan/CURRENT_STACK.md](../plan/CURRENT_STACK.md)
- 저장소 루트에서 시작합니다

## 포트 / 환경 변수

| 서비스 | 포트 | 주요 환경 변수 (기본값 있음) |
|--------|------|------------------------------|
| PostgreSQL | 5433 | `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` |
| storyforge-ai | 8001 (127.0.0.1) | `LLM_PROVIDER=mock`, `LLM_MODEL`, `LLM_EFFORT`, `ANTHROPIC_API_KEY` |
| storyforge-backend | 8080 | `DB_URL`, `AI_SERVICE_URL`, `FRONTEND_ORIGIN`, `JWT_SECRET`, `JWT_TTL`, `ADMIN_EMAILS` (전체는 [CURRENT_STACK.md](../plan/CURRENT_STACK.md)) |
| storyforge-console | 4201 | `/api` 프록시 사용. `CONSOLE_ORIGIN`, `ADMIN_EMAILS`는 백엔드에 주입 |
| storyforge-frontend | 4200 | (없음. `/api`는 프록시) |

개발 기본값으로 실행할 수 있습니다. 변수 키는 루트 `.env.example`을 참고합니다. Compose는 루트 `.env`를 치환에 사용하고 FastAPI는 `.env`를 직접 읽습니다. **Spring Boot는 `.env` 파일을 자동으로 읽지 않습니다.** 백엔드는 실행 셸의 환경 변수 또는 배포 도구로 값을 주입한 뒤 시작합니다. 예를 들어 운영자 지정은 `docs/guidance/operations-console.md`의 PowerShell 설정을 따릅니다. Angular는 `/api` 프록시만 사용하며 DB·모델 자격 증명을 주입하지 않습니다.

## 명령

콘솔까지 실행하려면 터미널 5개에서 순서대로 실행합니다.

```bash
# 1. DB
docker compose up -d

# 2. AI 서비스 (최초 1회 venv 생성)
cd storyforge-ai
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt     # macOS/Linux: .venv/bin/python
.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001

# 3. 백엔드 (Flyway가 스키마를 자동 생성)
cd storyforge-backend
./mvnw spring-boot:run

# 4. 프론트엔드 (최초 1회 npm install)
cd storyforge-frontend
npm install
npx ng serve

# 5. 운영 콘솔 (선택)
cd storyforge-console
npm ci
npm start
```

실제 Claude로 생성하려면 2번을 이렇게 실행합니다.

```bash
LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... .venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

백엔드의 외부 모델 안내에도 같은 `LLM_PROVIDER` 값을 설정해야 합니다. 로컬 Gemma 실행과 측정은 [Ollama 가이드](ollama-performance.md)를 참고합니다.

## 기대결과

- `curl localhost:8001/health` → `{"status":"ok","provider":"mock",...}`
- `curl localhost:8080/actuator/health` → `{"status":"UP"}`
- 브라우저 `http://localhost:4200` → 로그인 화면. 이용약관에 동의하고 회원가입하면 내 프로젝트(`/`)로 이동
- 새 프로젝트 → Story Bible 생성·확정 → Outline 생성·확정 → 챕터 탭에서 생성 시 "AI writing..."과 함께 본문이 흘러나오고, 우측에 Continuity 점수가 표시됩니다
- mock 모드 생성물에는 "※ 개발용 mock 생성기 출력입니다" 표시가 붙습니다

## 테스트

```bash
cd storyforge-backend && ./mvnw test
cd storyforge-ai && .venv/Scripts/python -m pip install -r requirements-dev.txt && .venv/Scripts/python -m pytest -q
cd storyforge-frontend && npx ng test --watch=false && npx ng build
cd storyforge-console && npm test && npm run build
```

## 실패하면

- 백엔드가 `Connection refused ... 5433`으로 멈춤 → `docker compose ps`로 DB가 `healthy`인지 확인
- 챕터 생성 시 "챕터 생성에 실패했습니다" → AI 서비스(8001)가 떠 있는지 확인. 실패하면 상태는 생성 전으로 되돌아갑니다
- `health`가 JSON 대신 HTML을 돌려주거나 `Port ... already in use` → 다른 프로그램이 포트를 쓰고 있습니다. [../repeater/local-port-conflicts.md](../repeater/local-port-conflicts.md)
- 그 밖의 문제는 [../find_debug/](../find_debug/), 환경 문제는 [../repeater/](../repeater/)에 기록합니다
