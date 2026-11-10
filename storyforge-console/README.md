# storyforge-console

StoryForge 운영자 전용 Angular 21 앱입니다. 회원·프로젝트 현황, 신고 일괄 조치, 회원 정지·해제, 감사 기록을 제공합니다. 사용자용 창작 화면과 가입 화면은 포함하지 않습니다.

## 실행

Node.js는 [확정 스택](../docs/plan/CURRENT_STACK.md)을 따릅니다. 백엔드와 PostgreSQL을 먼저 시작하고, 기존 운영자 계정을 `ADMIN_EMAILS`에 등록하세요. 이메일 인증이 없으므로 운영자 계정을 먼저 확보해야 합니다.

```sh
npm ci
npm start
```

`http://localhost:4201`에서 접속합니다. 개발 서버는 `/api`를 백엔드 8080으로 전달합니다. 운영에서는 `dist/storyforge-console/browser`를 제공하고 `/api`를 Spring Boot로 프록시하며 SPA 경로를 `index.html`로 fallback해야 합니다. `CONSOLE_ORIGIN`은 실제 콘솔 주소로 지정합니다. 콘솔 호스팅이나 외부 배포는 별도 구성입니다.

## 검증

```sh
npm test
npm run build
```

권한·토큰 경계·만료 처리·신고 일괄 처리 UI를 검증합니다. 실서버 권한·원자성·동시 조치는 백엔드 통합 테스트로 확인합니다.

## 구조

| 폴더 | 역할 |
|------|------|
| `src/app/core` | API 계약, 인증·인터셉터·가드 |
| `src/app/features` | 로그인·현황·신고·회원·감사 기록 |
| `src/app/layout` | 운영 메뉴·세션 표시 |
| `src/app/shared` | 페이지 이동 |

모든 요청은 Spring Boot `/api/v1`을 사용합니다. LLM과 FastAPI를 직접 호출하지 않습니다. API 키나 DB 자격 증명을 프론트에 넣지 않습니다.

- [스펙](../docs/plan/operations-console.md)
- [운영 가이드](../docs/guidance/operations-console.md)
