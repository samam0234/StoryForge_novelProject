# storyforge-frontend/src/app/shared/components/pipeline-stepper/

프로젝트의 현재 파이프라인 단계를 7단계 칩으로 보여주는 컴포넌트입니다 (기획서 §9.1 "단계적 진행 가시화").

## 들어갈 것

- `pipeline-stepper.component.ts` — 입력: `status: ProjectStatus`

## 들어가지 않을 것

- 상태 전이 로직 (백엔드 `service/orchestration/` 담당)

## 관련 문서

- [docs/Architecture/pipeline-state-machine.md](../../../../../../docs/Architecture/pipeline-state-machine.md)
- 상위 폴더: [../README.md](../README.md)
