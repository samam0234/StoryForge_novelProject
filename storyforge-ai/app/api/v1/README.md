# storyforge-ai/app/api/v1/

**v1 내부 라우터**입니다. 요청을 검증하고 `services/`의 생성기·검증기를 호출합니다.

## 들어갈 것

- `bible.py` — Story Bible 생성 (예정)
- `outline.py` — Outline 생성 (예정)
- `chapter.py` — 챕터 생성 + Continuity 결과 반환 (예정)
- Spring Boot가 호출하는 경로 (예: `POST /internal/generate/chapter`, §7.2)

## 들어가지 않을 것

- 인증 로직 (내부망 전용, §11)
- 프롬프트 문자열 — `prompts/`에 둡니다

> 기획서는 내부 경로를 `/internal/generate/chapter`로 예시합니다. `api/v1/` 폴더와 URL prefix의 관계는 구현 스펙(`docs/plan/`)에서 확정합니다.

## 관련 문서

- [기획서 §7.2, §8.3, §11](../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
