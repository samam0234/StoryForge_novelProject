# storyforge-frontend/src/app/shared/

여러 기능에서 함께 쓰는 **공통 컴포넌트**를 둡니다. 서비스 상태를 가진 싱글톤은 `core/`에, 한 기능에서만 쓰는 컴포넌트는 해당 `features/*`에 둡니다.

## components/

| 폴더 | 내용 |
|------|------|
| `pipeline-stepper/` | 프로젝트의 현재 파이프라인 단계를 7단계 칩으로 표시 (기획서 §9.1 "단계적 진행 가시화"). 입력: `status` |
| `rich-text-editor/` | TipTap 에디터. 입력 `value`·`editable`, 출력 `valueChange`. **입출력은 Markdown** ([저장 형식 결정](../../../../docs/trainings/2026-11-02_chapter-content-markdown.md)) |
| `user-chip/`, `work-card/` | 유저 아바타·이름, 작품 카드 |
| `work-graph/` | 작업 그래프: 수동·에이전트 비율, 날짜별 막대, 기록 표, git 형식 작업 기록표. 막대는 그날 작업, 점은 기록 문단 |
| `commit-graph/` | 참여자 작업 커밋 기록표. 점을 누르면 메시지와 표시 구간 |
| `share-graph/` | 공유 그래프: 유저 → 채널 연결과 표 |
| `clone-network/` | 원작 → 파생작 트리와 Clone 기록 |
| `package-map/` | 패키지 작품 연결 지도 |
| `contribution-heatmap/` | 활동 잔디 |
| `version-history/` | 두 버전을 골라 단어 단위 Diff로 비교하고, 선택한 버전을 편집기로 불러오는 다이얼로그 |

## 규칙

- 차트 색은 `--sf-series-*` 토큰만 씁니다. 수동·에이전트 색은 앱 전체에서 같습니다. 색을 바꾸면 색약 검증을 다시 합니다.
- 차트는 색만으로 구분하지 않습니다. 범례와 표(또는 레이블)를 함께 둡니다.

- 컴포넌트는 API를 호출하지 않습니다. 데이터는 입력으로 받고 결과는 출력(또는 다이얼로그 결과)으로 돌려줍니다.
- 에디터와 이력의 "불러오기"는 **편집기에 채우기만** 하고, 저장은 사용하는 화면이 사용자 확인 뒤에 합니다.
- 사용자 입력을 HTML로 삽입하지 않습니다 (Diff도 텍스트로만 렌더링).
- 디자인 시스템 래퍼(`ui/`)는 아직 필요한 컴포넌트가 없어 만들지 않았습니다. 토큰과 테마는 `src/styles/`에 있습니다.

## 관련 문서

- [기획서 §9.1, §9.2](../../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/plan/should-features.md](../../../../docs/plan/should-features.md)
