# docs/Architecture/

시스템 구조, 오케스트레이션 흐름, 상태 머신을 그림으로 남기는 폴더입니다.

## 원칙

- **구현이 바뀌면 여기를 먼저 갱신합니다.** 코드와 다이어그램이 어긋나지 않게 합니다.
- 다이어그램은 ASCII 또는 mermaid로 그립니다. 이미지 파일만 두지 않습니다.
- 기획서의 상태 머신과 파이프라인 단계는 임의로 바꾸지 않습니다. 기획서에 없는 전이는 "미정 사항"으로 따로 적습니다.

## 문서 목록

| 문서 | 내용 | 기준 절 |
|------|------|---------|
| [system-overview.md](system-overview.md) | 고수준 구조, 서비스 책임, 호출 규칙, 챕터 생성 흐름 | 기획서 §7 |
| [pipeline-state-machine.md](pipeline-state-machine.md) | 프로젝트 단위 파이프라인 상태 머신 | 기획서 §7.2 |

## 들어가지 않을 것

- 버전, 포트 같은 확정값 → [../plan/CURRENT_STACK.md](../plan/CURRENT_STACK.md)
- API 필드 단위 스펙 → [../plan/](../plan/)
