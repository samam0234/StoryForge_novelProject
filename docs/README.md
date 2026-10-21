# docs/ — StoryForge 문서 진입점

StoryForge의 개발 스펙, 아키텍처, 커밋 기록, 운영 문서를 모아 두는 곳입니다.
기획 원본은 여기 두지 않고 루트의 [StoryForge_기획서_v1.2.md](../StoryForge_기획서_v1.2.md)에 그대로 둡니다.

## 폴더 안내

| 폴더 | 용도 | 언제 쓰나 |
|------|------|-----------|
| [plan/](plan/) | 기획서에서 파생된 개발 스펙, 확정 스택 | 기능을 구현하기 전에 스펙을 정리할 때 |
| [Architecture/](Architecture/) | 시스템 구조, 오케스트레이션 흐름, 상태 머신 | 구조가 바뀔 때 (구현보다 먼저 갱신) |
| [branchs/](branchs/) | 브랜치·커밋 기록 | **커밋할 때마다** |
| [guidance/](guidance/) | 실행·기능 가이드 | 다른 사람이 따라 할 절차가 생겼을 때 |
| [find_debug/](find_debug/) | 날짜별 디버그 일지 | 문제를 조사하고 해결했을 때 |
| [repeater/](repeater/) | 재발 가능한 인프라 이슈 | 같은 환경 문제가 다시 생길 수 있을 때 |
| [vaildates/](vaildates/) | 배포·데모 전 수동 검증 체크리스트 | 배포나 데모 직전 |
| [web_management/](web_management/) | 배포 전/중/후 문서, 포트 인벤토리 | 배포 환경을 다룰 때 |
| [trainings/](trainings/) | 설계 판단과 연구 노트 ("왜"만) | 중요한 선택의 이유를 남길 때 |
| [novel/](novel/) | 세계관 템플릿, 프롬프트 가이드, Continuity 규칙 | 창작 파이프라인 규칙을 정리할 때 |

> `vaildates/`의 스펠링은 의도적입니다. 교정하지 않습니다.

## 어디에 써야 할지 모를 때

```
기획서 내용을 개발 스펙으로 풀어 쓴다        → plan/
서비스 구조나 상태 전이가 바뀐다             → Architecture/
커밋을 했다                                → branchs/commits/
"이렇게 실행하세요" 절차를 적는다            → guidance/
오늘 버그를 잡았다                          → find_debug/
환경 문제가 또 생길 것 같다                  → repeater/
배포 전에 손으로 확인할 목록이다              → vaildates/
포트, 도메인, 배포 단계 기록이다              → web_management/
"왜 A 대신 B를 골랐는지" 남긴다              → trainings/
Story Bible 템플릿, 프롬프트 작성 규칙이다    → novel/
```

## 공통 규칙

- 원본과 모순되면 **기획 원본이 우선**하고, 파생 문서를 고칩니다.
- 구현을 바꾸면 [Architecture/](Architecture/)와 [plan/CURRENT_STACK.md](plan/CURRENT_STACK.md)를 함께 갱신합니다.
- 커밋 기록은 `docs/branchs/commits/`에만 둡니다. `docs/commits/`는 만들지 않습니다.
- 새 하위 폴더가 필요하면 먼저 정본 [SKILL.md](../.agents/skills/storyforge/SKILL.md)의 docs 표에 용도를 추가한 뒤 만들고, 이 표도 갱신합니다.
- 모든 폴더에 `README.md`를 둡니다.
