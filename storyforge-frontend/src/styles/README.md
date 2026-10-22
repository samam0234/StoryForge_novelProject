# storyforge-frontend/src/styles/

**글로벌 스타일과 Light/Dark 테마**를 둡니다. Angular Material 커스텀 테마도 여기서 정의합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `_tokens.scss` | 디자인 토큰 (`--sf-*` CSS 변수, `light-dark()`) |
| `_theme.scss` | Angular Material 테마와 시스템 토큰 오버라이드 |

## 들어갈 것

- 디자인 토큰 (색상, 타이포, 간격)
- Material 커스텀 테마 (light/dark)

## 들어가지 않을 것

- 컴포넌트 전용 스타일 → 각 컴포넌트

## 기획서 컬러 팔레트 (§9.3)

| 용도 | 컬러 | 비고 |
|------|------|------|
| Primary | #1F4E79 | 딥 블루 (신뢰, 창작) |
| Secondary | #2E75B6 | 미디엄 블루 |
| Accent | #E8A838 | 따뜻한 골드 (강조, CTA) |
| Background (Light) | #F8F9FA | |
| Background (Dark) | #121212 | |
| Surface | #FFFFFF / #1E1E1E | |
| Text Primary | #1A1A1A / #E0E0E0 | |

타이포그래피: 본문 및 UI는 Pretendard (또는 Noto Sans KR), 제목은 같은 패밀리에서 weight로 구분 (§9.4).

## 관련 문서

- [기획서 §9.3, §9.4](../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
