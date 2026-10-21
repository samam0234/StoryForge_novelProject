# storyforge-frontend/src/app/shared/

여러 기능에서 함께 쓰는 **공통 컴포넌트, 파이프, 디렉티브**를 둡니다.

## 들어갈 것

- 기능 두 곳 이상에서 쓰는 UI 조각

## 들어가지 않을 것

- 서비스 상태를 가진 싱글톤 → `core/`
- 한 기능에서만 쓰는 컴포넌트 → 해당 `features/*`

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [components/](components/) | 공통 컴포넌트 |
| [ui/](ui/) | 디자인 시스템 컴포넌트 |

## 관련 문서

- [기획서 §8.1](../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
