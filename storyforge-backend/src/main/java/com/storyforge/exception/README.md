# storyforge-backend/src/main/java/com/storyforge/exception/

**예외 타입과 전역 예외 처리기**를 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `GlobalExceptionHandler.java` | 예외 → ProblemDetail (404, 409, 400, 502) |
| `NotFoundException.java` | 404 |
| `InvalidStateException.java` | 409 (현재 상태에서 불가) |
| `AiServiceException.java` | 502 (AI 서비스 장애) |

## 들어갈 것

- 도메인 예외, 전역 예외 핸들러, 표준 에러 응답
- FastAPI 장애·타임아웃을 사용자에게 명확히 전달 (§15)

## 들어가지 않을 것

- 스택 트레이스, 내부 주소를 외부 응답에 노출

## 관련 문서

- [기획서 §8.2, §15](../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
