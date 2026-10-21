# storyforge-backend/src/main/java/com/storyforge/

백엔드의 **루트 패키지**입니다. 기획서 §8.2의 계층 구조를 따릅니다.

## 들어갈 것

- `StoryforgeApplication.java` — 애플리케이션 진입점 (예정)

## 들어가지 않을 것

- 계층 밖에 흩어진 클래스 — 아래 하위 패키지 중 하나에 넣습니다

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [client/](client/) | FastAPI 클라이언트 (WebClient) |
| [config/](config/) | 설정 클래스 |
| [controller/](controller/) | REST Controllers |
| [domain/](domain/) | Entity, DTO, Repository |
| [exception/](exception/) | 예외 처리 |
| [service/](service/) | 비즈니스 로직 |

## 의존 방향

```
controller → service(orchestration, project, bible, chapter) → domain(repository)
                    └→ client (FastAPI)
```

## 관련 문서

- [기획서 §8.2](../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
