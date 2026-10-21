# storyforge-backend/src/main/java/com/storyforge/client/

내부망의 **FastAPI AI Service를 호출하는 WebClient 클라이언트**입니다. 백엔드에서 FastAPI로 가는 호출은 모두 여기를 거칩니다.

## 들어갈 것

- 내부 엔드포인트 호출 (예: `POST /internal/generate/chapter`, §7.2)
- 스트리밍 응답 수신
- 타임아웃, 재시도, Circuit Breaker, 명확한 에러 전파 (§15)

## 들어가지 않을 것

- LLM Provider 직접 호출
- 상태 전이 판단 → `service/orchestration/`

## 관련 문서

- [기획서 §7.2, §15](../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
