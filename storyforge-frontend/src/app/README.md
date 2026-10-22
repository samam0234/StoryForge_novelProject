# storyforge-frontend/src/app/

애플리케이션 코드가 들어가는 폴더입니다. 역할에 따라 `core`, `shared`, `features`, `layout`으로 나눕니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `app.ts` | 루트 컴포넌트. `sf-shell` 레이아웃만 렌더링 |
| `app.config.ts` | Router(컴포넌트 입력 바인딩), HttpClient(fetch) 등록 |
| `app.routes.ts` | 최상위 라우트: `/`, `/projects/:projectId/{bible,outline,chapters}` (지연 로딩) |

## 들어갈 것

- `app.routes.ts` — 최상위 라우트 정의
- `app.config.ts`, 루트 컴포넌트 (CLI 초기화 시 생성)

## 들어가지 않을 것

- 기능별 화면을 이 폴더 바로 아래에 두지 않습니다 → `features/`

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [core/](core/) | 싱글톤 서비스, 인터셉터, 가드 |
| [features/](features/) | 기능 단위 화면 |
| [layout/](layout/) | 레이아웃 |
| [shared/](shared/) | 공통 컴포넌트, 파이프, 디렉티브 |

## 관련 문서

- [기획서 §8.1](../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
