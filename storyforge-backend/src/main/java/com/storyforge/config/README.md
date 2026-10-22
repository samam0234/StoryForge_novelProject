# storyforge-backend/src/main/java/com/storyforge/config/

**Security, Web, OpenAPI, WebClient** 설정 클래스를 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `SecurityConfig.java` | CORS, 무상태 세션, 경로별 허용 (인증 도입 전 `/api/v1/**` 개방) |
| `WebClientConfig.java` | FastAPI용 WebClient (연결 5초, 응답 타임아웃) |
| `OpenApiConfig.java` | OpenAPI 문서 정보 |

## 들어갈 것

- Spring Security (JWT 필터, 인가 규칙)
- CORS 설정 (§13)
- OpenAPI 설정 (§11)
- FastAPI용 WebClient 빈 (타임아웃 포함)
- Rate Limiting (§13)

## 들어가지 않을 것

- 포트·주소 하드코딩 — `application.yml`과 환경 변수로 주입

## 관련 문서

- [기획서 §8.2, §13](../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
