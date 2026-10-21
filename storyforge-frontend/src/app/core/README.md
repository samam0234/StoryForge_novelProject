# storyforge-frontend/src/app/core/

앱 전체에서 한 번만 생성되는 **싱글톤 서비스, HTTP 인터셉터, 라우트 가드**를 둡니다.

## 들어갈 것

- 인증 상태, API 클라이언트, 공용 모델

## 들어가지 않을 것

- 특정 기능에서만 쓰는 서비스 → 해당 `features/*`
- 화면 컴포넌트

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [api/](api/) | HTTP 클라이언트 래퍼 |
| [auth/](auth/) | 인증 (JWT) |
| [models/](models/) | 공용 타입 모델 |

## 관련 문서

- [기획서 §8.1](../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
