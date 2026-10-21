# storyforge-backend/src/main/java/com/storyforge/domain/

도메인별 **Entity, DTO, Repository**를 둡니다. 기획서 §10 데이터 모델 초안을 기준으로 합니다.

## 들어갈 것

- JPA Entity, 요청/응답 DTO, Repository 인터페이스

## 들어가지 않을 것

- 비즈니스 로직 → `service/`

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [bible/](bible/) | Story Bible 도메인 |
| [chapter/](chapter/) | Chapter 도메인 |
| [outline/](outline/) | Outline 도메인 |
| [project/](project/) | Project 도메인 |

## 관련 문서

- [기획서 §8.2, §10](../../../../../../../StoryForge_기획서_v1.2.md)
- db/migration (스키마)
- 상위 폴더: [../README.md](../README.md)
