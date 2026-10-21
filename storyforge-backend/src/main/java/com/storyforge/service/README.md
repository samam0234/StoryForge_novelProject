# storyforge-backend/src/main/java/com/storyforge/service/

**비즈니스 로직** 계층입니다. 파이프라인 오케스트레이션과 도메인별 서비스로 나뉩니다.

## 들어갈 것

- 트랜잭션 경계
- 도메인 규칙 검증

## 들어가지 않을 것

- HTTP 요청/응답 처리 → `controller/`
- FastAPI HTTP 세부 구현 → `client/`

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [bible/](bible/) | Story Bible 서비스 |
| [chapter/](chapter/) | 챕터 서비스 |
| [orchestration/](orchestration/) | 파이프라인 오케스트레이션 & 상태 머신 |
| [project/](project/) | 프로젝트 서비스 |

> 기획서 §8.2의 `service/` 아래에는 `outline/` 패키지가 없습니다 (`orchestration`, `project`, `bible`, `chapter`만 있음).
> Outline 비즈니스 로직을 어디에 둘지는 구현 전에 확인합니다. 임의로 패키지를 추가하지 않았습니다.

## 관련 문서

- [기획서 §8.2](../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
