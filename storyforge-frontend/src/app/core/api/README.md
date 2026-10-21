# storyforge-frontend/src/app/core/api/

Spring Boot API(`/api/v1/`)를 호출하는 **HTTP 클라이언트 래퍼**입니다. 화면 코드는 `HttpClient`를 직접 쓰지 않고 이 래퍼를 씁니다.

## 들어갈 것

- REST 호출 래퍼 (기본 URL은 `environments/`에서)
- 챕터 생성 SSE 수신 래퍼
- 공통 에러 처리

## 들어가지 않을 것

- **FastAPI 주소, `/internal/*` 호출** — Angular는 FastAPI를 직접 호출하지 않습니다
- LLM SDK, 프롬프트

## 관련 문서

- [기획서 §11 (주요 엔드포인트)](../../../../../StoryForge_기획서_v1.2.md)
- 정본 "호출 및 책임 규칙"
- 상위 폴더: [../README.md](../README.md)
