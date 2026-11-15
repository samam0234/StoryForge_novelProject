# docs/images/

루트 [README](../../README.md)가 쓰는 화면 캡처, GIF, 영상, 로고, 배너, 다이어그램을 두는 폴더입니다. 모두 [readme-assets 도구](../../storyforge-frontend/scripts/readme-assets/)로 다시 만들 수 있습니다.

## 무엇이 들어 있나

| 위치 | 내용 | 만드는 단계 |
|------|------|-------------|
| `hero.png` | README 맨 위 배너 (1600×760) | `brand hero` |
| `social-preview.png` | GitHub 저장소 소셜 미리보기용 (1280×640). 저장소 Settings → Social preview에 올려 씁니다 | `brand hero` |
| `pipeline.png` | 창작 파이프라인 6단계 | `brand pipeline` |
| `architecture-logos.png` | 서비스 구조와 통신 경로 | `brand architecture` |
| `stack.png` | 계층별 기술 스택 | `brand stack` |
| `logos/*.png` | 기술 로고 타일 20개 (192×192, 투명 배경) | `brand logos` |
| `logos/src/*.svg` | 로고 원본 SVG ([Simple Icons](https://simpleicons.org/)) | 직접 받아 둠 |
| `screens/*.png` | 실제 앱 화면을 브라우저 창 틀에 넣은 캡처 | `capture` |
| `demo/generate.gif`, `.mp4` | 챕터 생성 스트리밍 장면 | `capture` |
| `demo/community.gif` | 작품 화면의 탭 순회 | `capture` |
| `demo/console.gif` | 운영 콘솔 순회 | `capture` |
| `demo/tour.mp4` | 10개 화면 투어 (33초, H.264) | `capture` |
| `demo/tour-poster.png` | 투어 영상으로 연결하는 재생 버튼 포스터 | `brand poster` |

GitHub README는 저장소 안의 mp4를 문서 안에서 바로 재생하지 못합니다. 그래서 GIF로 보여 주고, 영상은 포스터 이미지를 눌러 여는 링크로 둡니다.

## 무엇을 찍었나 (꼭 읽어 주세요)

- **샘플 데이터:** 화면 속 작가 9명, 작품, 댓글, 라운지 글은 소개를 위해 **직접 쓴 가상의 샘플**입니다([data.mjs](../../storyforge-frontend/scripts/readme-assets/data.mjs)). 실제 사용자 데이터가 아니고, 언어 모델이 쓴 글도 아닙니다.
- **mock 생성기:** 시드와 챕터 생성 장면은 `LLM_PROVIDER=mock`으로 찍었습니다. Bible·Outline·챕터를 mock으로 생성한 뒤 샘플 문장으로 덮어쓰고 승인했습니다. 생성 GIF에는 mock 표시가 그대로 보입니다. **실제 모델의 문장 품질을 보여 주는 자료가 아닙니다.**
- **실제 동작:** 화면, 그래프, Continuity 점수와 이슈(규칙 검사), 패키지 검사, 신고 처리 화면은 실제 앱이 그 데이터로 그린 것입니다. 다만 활동 잔디와 조회수가 자연스럽게 보이도록 **데모 DB에서만** 활동 날짜를 최근 300일에 흩뿌리고 조회수를 조정했습니다([seed.mjs](../../storyforge-frontend/scripts/readme-assets/seed.mjs)의 `spreadDates`).
- **URL 표시:** 창 틀의 주소는 로컬 개발 주소(`localhost:4200`, `localhost:4201`) 그대로입니다. 공개 배포된 서비스가 아닙니다.

## 다시 만들기

로컬 개발 DB를 건드리지 않도록 **별도의 데모 DB**를 씁니다.

### 1. 준비

| 필요한 것 | 비고 |
|-----------|------|
| 실행 중인 Docker와 PostgreSQL 컨테이너 | 루트에서 `docker compose up -d` |
| Chrome | playwright-core가 설치된 Chrome을 씁니다 (없으면 Edge) |
| ffmpeg | 시스템에 없으면 `pip install --target <폴더> imageio-ffmpeg` 후 그 안의 실행 파일 경로를 `FFMPEG`로 넘깁니다 |
| 인터넷 | 표지·배너의 글꼴(Pretendard, Noto Serif KR, Material Icons)을 CDN에서 불러옵니다 |

```bash
# 데모 DB 만들기 (한 번)
docker exec storyforge-postgres psql -U storyforge -d postgres -c "CREATE DATABASE storyforge_demo"
```

### 2. 스택 실행 (데모 설정)

```bash
# AI: mock, 스트리밍이 보이도록 조금 느리게
cd storyforge-ai && MOCK_STREAM_DELAY=0.05 LLM_PROVIDER=mock .venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001

# 백엔드: 데모 DB, 관리자, 시드가 막히지 않도록 요청 제한을 넉넉히
cd storyforge-backend && DB_URL=jdbc:postgresql://localhost:5433/storyforge_demo ADMIN_EMAILS=admin@example.com \
  RATE_LIMIT_AUTH_PER_MINUTE=5000 RATE_LIMIT_GENERATE_PER_MINUTE=5000 RATE_LIMIT_COMMUNITY_PER_MINUTE=5000 ./mvnw spring-boot:run

# 사용자 웹과 운영 콘솔
cd storyforge-frontend && npx ng serve
cd storyforge-console && npm start
```

### 3. 생성

```bash
cd storyforge-frontend
npm install --no-save playwright-core
node scripts/readme-assets/index.mjs seed                 # 샘플 데이터 (빈 데모 DB에서 한 번)
FFMPEG=<ffmpeg 경로> node scripts/readme-assets/index.mjs capture
node scripts/readme-assets/index.mjs brand                # 로고, 스택, 파이프라인, 아키텍처, 배너, 포스터
```

- `ONLY=board,chapter node ... capture`처럼 화면 이름을 주면 그 정지 화면만 다시 찍습니다 (GIF와 영상은 건너뜀).
- `brand hero poster`처럼 단계를 골라 실행할 수 있습니다. `hero`와 `poster`는 `screens/`가 먼저 있어야 합니다.
- **시드는 빈 데모 DB에서 한 번만** 실행합니다. 다시 하려면 데모 DB를 지우고 만듭니다. 챕터 생성 GIF는 4장을 실제로 생성하므로, `capture`를 다시 하려면 시드부터 다시 해야 같은 화면이 나옵니다.
- 시드가 만드는 `scripts/readme-assets/.state.json`(데모 계정 ID)은 Git에서 제외합니다.

## 라이선스

- 화면 캡처와 배너, 다이어그램은 이 저장소의 [LICENSE](../../LICENSE)를 따릅니다.
- `logos/`의 아이콘은 [Simple Icons](https://simpleicons.org/)(CC0 1.0)이고 **각 상표는 해당 소유자에게 있습니다.** 이 프로젝트가 쓰는 기술을 표시하는 용도로만 쓰며, 보증이나 제휴를 뜻하지 않습니다.
- 글꼴은 [THIRD_PARTY_NOTICES.md](../plan/Proposal/THIRD_PARTY_NOTICES.md)에 적었습니다.

## 규칙

- 새 화면을 README에 넣을 때는 손으로 찍지 말고 도구에 추가해 다시 만들 수 있게 합니다.
- 실제 사용자 데이터, 시크릿, 생성된 실제 원고를 찍지 않습니다.
- 화면이 바뀌어 캡처가 낡으면 다시 만들고, README의 설명도 함께 고칩니다.
