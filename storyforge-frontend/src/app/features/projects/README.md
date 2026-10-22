# storyforge-frontend/src/app/features/projects/

프로젝트 **목록 조회와 신규 생성** 화면입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `project-shell.page.ts` | 프로젝트 상단(제목, 파이프라인 단계) + Bible/Outline/챕터 탭 |
| `project-context.ts` | 프로젝트·상태 신호를 하위 화면과 공유 (화면 단위 provider) |
| `project-create.dialog.ts` | 새 프로젝트 입력 다이얼로그 |

## 들어갈 것

- 프로젝트 목록, 생성 폼 (`POST /api/v1/projects`)

## 들어가지 않을 것

- 대시보드 카드 그리드 → `dashboard/`

> 프로젝트 설정 화면(§9.6 5번: 모델 선택, 기본 톤, 내보내기 옵션)의 위치는 기획서 §8.1에 정해져 있지 않습니다. 구현 전에 확인합니다.

## 관련 문서

- [기획서 §8.1, §11](../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
