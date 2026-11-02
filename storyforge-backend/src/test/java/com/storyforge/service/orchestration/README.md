# storyforge-backend/src/test/java/com/storyforge/service/orchestration/

파이프라인 상태 머신과 오케스트레이션 테스트입니다.

## 들어갈 것

- `PipelineIntegrationTest` — 전체 파이프라인, 컨텍스트 주입, 단계·검토 건너뛰기 차단, 재생성, 실패 시 상태 복귀, 내부 정보 비노출
- `PipelineStateMachineTest` — 기획서 §7.2 순서대로만 전이되는지, 단계 건너뛰기가 막히는지, v1.3 추가 전이(챕터 반복·실패 복귀)가 허용되는지

## 관련 문서

- [docs/Architecture/pipeline-state-machine.md](../../../../../../../../docs/Architecture/pipeline-state-machine.md)
- 상위 폴더: [../README.md](../README.md)
