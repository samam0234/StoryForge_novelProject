# feat(web): Angular 기초 화면 구현

## 제목 / 커밋번호

- **제목**: `feat(web): Angular 기초 화면 구현`
- **커밋번호**: `c21d882`
- **브랜치**: `feature/web-foundation`
- **일시**: 2026-10-22 03:43

## 주 내용

Angular 21 + Angular Material로 대시보드, Story Bible, Outline, 챕터 워크스페이스 화면을 만들었습니다.

## 상세

- Angular CLI 21.2.27로 생성 (22는 Node 24.15+ 필요로 보류), Material 21 추가
- `core/api`: Spring Boot `/api/v1` 래퍼와 fetch 기반 챕터 SSE 수신. FastAPI 주소는 넣지 않음
- `styles/`: 기획서 §9.3 팔레트 토큰, color-scheme 기반 Light/Dark 테마 (다크 기본)
- `features/`: 대시보드(§9.6-1), Bible 탭 편집(§9.6-2), Outline 드래그 앤 드롭(§9.6-3), 챕터 3단 워크스페이스(§9.6-4)
- `shared/components/pipeline-stepper`: 파이프라인 7단계 표시 (§9.1)
- high 심각도 Continuity 이슈가 있으면 승인 전 재확인 (결과를 자동 통과시키지 않음)

## 결과

- `npx ng build` 경고 없이 성공, `ng serve`에서 `/api` 프록시로 백엔드 응답 확인
- 브라우저 화면 확인은 사용자 몫으로 남김 (자동 스크린샷 미실시)
- 남은 작업: 인증 화면, TipTap 에디터, 내보내기, 프로젝트 설정 화면
