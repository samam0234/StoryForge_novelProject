# storyforge-backend/src/main/java/com/storyforge/controller/

외부에 노출되는 **REST Controller**입니다. 모든 경로는 `/api/v1/` 아래에 둡니다.

## 들어갈 것

- 기획서 §11 주요 엔드포인트:
  - `POST /api/v1/projects`, `POST /api/v1/projects/{id}/bible/generate`, `PUT /api/v1/projects/{id}/bible`
  - `POST /api/v1/projects/{id}/outline/generate`, `POST /api/v1/projects/{id}/chapters/{num}/generate`
  - `PUT /api/v1/chapters/{id}/confirm`, `GET /api/v1/projects/{id}/status`
- 챕터 생성 SSE 응답

## 들어가지 않을 것

- 비즈니스 로직, 상태 전이 — `service/`에 위임합니다
- FastAPI 응답을 가공 없이 그대로 외부에 노출

## 관련 문서

- [기획서 §11](../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
