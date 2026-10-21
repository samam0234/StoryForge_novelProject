# storyforge-frontend/src/environments/

개발/운영 등 **환경별 설정**을 둡니다. 주로 Spring Boot API의 기본 URL입니다.

## 들어갈 것

- `environment.ts` 등 환경 파일 (CLI 초기화 시 생성)

## 들어가지 않을 것

- **FastAPI 주소**
- API Key 등 시크릿 (프론트 번들에 노출됨)
- 확정되지 않은 포트 — 값은 `docs/plan/CURRENT_STACK.md`가 원본

## 관련 문서

- [docs/plan/CURRENT_STACK.md](../../../docs/plan/CURRENT_STACK.md)
- 상위 폴더: [../README.md](../README.md)
