# storyforge-ai/app/models/

요청·응답을 정의하는 **Pydantic v2 스키마**를 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `schemas.py` | Pydantic 스키마 (JSON은 camelCase alias) |

## 들어갈 것

- Spring Boot가 보내는 컨텍스트 패키지 스키마
- 생성 결과 + Continuity 리포트 스키마 (issues: type, description, severity / score, §10)

## 들어가지 않을 것

- DB 모델 (영속화는 Spring Boot)

## 관련 문서

- [기획서 §8.3, §10, §12](../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
