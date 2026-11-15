# StoryForge 문서

StoryForge의 기획, 스펙, 구조, 실행 방법, 검증 기록, 문제 해결 기록을 모아 둔 곳입니다. 이 문서가 진입점입니다.

문서는 **무엇을 답하는가**에 따라 폴더가 나뉩니다. "무엇을 만드는가"는 `plan/`, "어떻게 생겼는가"는 `Architecture/`, "어떻게 실행하는가"는 `guidance/`, "정말 되는가"는 `vaildates/`, "왜 그렇게 정했는가"는 `trainings/`, "무엇이 잘못됐었는가"는 `find_debug/`에 있습니다.

> **기획 원본**은 [plan/Proposal/StoryForge_기획서_v1.2.md](plan/Proposal/StoryForge_기획서_v1.2.md)(문서 버전 1.3)입니다. 모든 결정의 최우선 기준이며, 다른 문서와 어긋나면 **원본이 이기고 파생 문서를 고칩니다.** 원본의 설계 의도, 워딩, 파이프라인 단계, 상태 머신은 사용자 확인 없이 고치지 않습니다.

## 목차

- [처음이라면](#처음이라면)
- [폴더 안내](#폴더-안내)
- [전체 문서 목록](#전체-문서-목록)
- [어디에 써야 할지 모를 때](#어디에-써야-할지-모를-때)
- [문서를 쓰는 규칙](#문서를-쓰는-규칙)
- [저장소의 다른 문서](#저장소의-다른-문서)

---

## 처음이라면

하려는 일에 맞는 순서로 읽으세요.

| 하려는 일 | 읽는 순서 |
|-----------|-----------|
| **프로젝트가 무엇인지 알고 싶다** | [루트 README](../README.md) → [기획서](plan/Proposal/StoryForge_기획서_v1.2.md) → [시스템 개요](Architecture/system-overview.md) |
| **로컬에서 실행하고 싶다** | [로컬 실행](guidance/local-dev-setup.md) → 막히면 [포트 충돌](repeater/local-port-conflicts.md) |
| **기능을 고치거나 추가한다** | [작업 규칙](../AGENTS.md) → [확정 스택](plan/CURRENT_STACK.md) → 해당 기능 스펙(`plan/`) → [API](plan/api-v1.md) |
| **창작 파이프라인을 건드린다** | [상태 머신](Architecture/pipeline-state-machine.md) → [Continuity 규칙](novel/continuity-rules.md) → [재검증 설계 판단](trainings/2026-11-04_review-and-moderation.md) |
| **운영자로 쓴다** | [운영 콘솔 가이드](guidance/operations-console.md) → [커뮤니티 운영 가이드](guidance/community-guide.md) → [신고·관리 스펙](plan/moderation.md) |
| **실제 언어 모델을 붙인다** | [Ollama 설정과 실측](guidance/ollama-performance.md) → [추론 처리의 선택 이유](trainings/2026-11-10_console-and-inference.md) |
| **메일 기능을 켠다** | [계정 메일 가이드](guidance/account-mail.md) → [인증 스펙](plan/auth.md) |
| **배포를 준비한다** | [보안 체크리스트](../SECURITY.md) → [포트 인벤토리](web_management/port-inventory.md) → [배포 전 체크리스트](vaildates/README.md) |
| **지금 어디까지 됐는지 알고 싶다** | [진행 현황](plan/roadmap-progress.md) → [검증 기록](vaildates/README.md) → [변경 내역](../CHANGELOG.md) |

## 폴더 안내

| 폴더 | 답하는 질문 | 두는 것 | 언제 쓰나 |
|------|-------------|---------|-----------|
| [plan/](plan/README.md) | 무엇을 만드는가 | 기획서에서 파생된 개발 스펙, 확정 스택, 진행 현황 | 기능을 구현하기 전에 스펙을 정리할 때 |
| [Architecture/](Architecture/README.md) | 어떻게 생겼는가 | 시스템 구조, 오케스트레이션 흐름, 상태 머신 | 구조가 바뀔 때 (구현보다 먼저 갱신) |
| [guidance/](guidance/README.md) | 어떻게 실행하는가 | 실행·기능 가이드. 전제 → 명령 → 기대 결과 | 다른 사람이 따라 할 절차가 생겼을 때 |
| [vaildates/](vaildates/README.md) | 정말 되는가 | 배포·데모 전 수동 검증 체크리스트와 결과 | 배포나 데모 직전, 검증을 마쳤을 때 |
| [trainings/](trainings/README.md) | 왜 그렇게 정했는가 | 설계 판단과 연구 노트. "왜"만 | 중요한 선택의 이유를 남길 때 |
| [find_debug/](find_debug/README.md) | 무엇이 잘못됐었는가 | 날짜별 디버그 일지. 증상, 원인, 해결, 재발 방지 | 문제를 조사하고 해결했을 때 |
| [repeater/](repeater/README.md) | 또 생길 문제는 무엇인가 | 재발 가능한 환경·인프라 이슈 | 같은 환경 문제가 다시 생길 수 있을 때 |
| [web_management/](web_management/README.md) | 어디에 떠 있는가 | 배포 전·중·후 기록, 포트 인벤토리 | 배포 환경을 다룰 때 |
| [novel/](novel/README.md) | 좋은 소설의 규칙은 무엇인가 | 세계관 템플릿, 프롬프트 가이드, Continuity 규칙 | 창작 파이프라인 규칙을 정리할 때 |
| [branchs/](branchs/README.md) | 언제 무엇을 커밋했는가 | 브랜치 목록, 커밋 기록 | 브랜치가 바뀔 때, 기록이 필요할 때 |
| [images/](images/) | — | 루트 README가 쓰는 구조·프레임워크 소개 이미지(PNG) | 소개 그림을 바꿀 때 |

> `vaildates/`의 스펠링은 의도적입니다. 교정하지 않습니다.

## 전체 문서 목록

### plan/ — 스펙과 기준값

| 문서 | 내용 |
|------|------|
| [Proposal/StoryForge_기획서_v1.2.md](plan/Proposal/StoryForge_기획서_v1.2.md) | **기획 원본** |
| [Proposal/THIRD_PARTY_NOTICES.md](plan/Proposal/THIRD_PARTY_NOTICES.md) | 제3자 라이선스 고지 |
| [CURRENT_STACK.md](plan/CURRENT_STACK.md) | 확정 버전, 포트, 환경 변수, 서비스 간 통신. **값을 바꿀 때 먼저 고치는 문서** |
| [roadmap-progress.md](plan/roadmap-progress.md) | 작업 순서, 결정 사항, 결정 대기, 진행 방식 변경 이력 |
| [api-v1.md](plan/api-v1.md) | 외부 API, 내부 API, SSE 이벤트, 웹소켓, Continuity 리포트 형식 |
| [auth.md](plan/auth.md) | JWT 인증, 소유자 검사, 이메일 인증·비밀번호 재설정 |
| [stability.md](plan/stability.md) | 요청 제한, Circuit Breaker, 멈춘 상태 복구, 추론 과부하 보호 |
| [should-features.md](plan/should-features.md) | 내보내기, 버전 이력·Diff, 프로젝트 설정 |
| [community.md](plan/community.md) | 게시판, 게시, Clone, 공유, 작업 그래프, 패키지포지, 이슈, 프로필, 가이드챗 |
| [moderation.md](plan/moderation.md) | 신고, 관리자 조치, 정지, 차단, 이용약관, 알림 |
| [operations-console.md](plan/operations-console.md) | 독립 운영 콘솔의 화면, API, 트랜잭션과 감사 |

### Architecture/ — 구조

| 문서 | 내용 |
|------|------|
| [system-overview.md](Architecture/system-overview.md) | 고수준 구조, 서비스 책임, 호출 규칙, 챕터 생성 흐름, 실시간 경로, AI 서버 내부 실행 조정 |
| [pipeline-state-machine.md](Architecture/pipeline-state-machine.md) | 프로젝트 상태와 전이 규칙, 실패 시 복귀 |
| [operations-console.md](Architecture/operations-console.md) | 운영 조치의 잠금과 원자성, Ollama 연결과 동시성 |

### guidance/ — 실행 가이드

| 문서 | 내용 |
|------|------|
| [local-dev-setup.md](guidance/local-dev-setup.md) | DB, AI, 백엔드, 사용자 웹, 콘솔 실행과 테스트 |
| [community-guide.md](guidance/community-guide.md) | 관리자 지정, 게시·Clone·신고·패키지 검사 확인 절차 |
| [operations-console.md](guidance/operations-console.md) | 운영 콘솔 실행, 조치와 오류 확인 |
| [account-mail.md](guidance/account-mail.md) | SMTP 이메일 인증·재설정 설정과 로컬 검증, Gmail 설정 |
| [ollama-performance.md](guidance/ollama-performance.md) | Gemma 설정, 지연 측정, 서버 조정 순서, 실측 결과와 한계 |

### vaildates/ — 검증 기록

| 문서 | 내용 | 상태 |
|------|------|------|
| [docs-overhaul-2026-11-15.md](vaildates/docs-overhaul-2026-11-15.md) | 문서 개편 전 자동 검사 전체와 발견 사항 | 2026-11-15 |
| [community-release.md](vaildates/community-release.md) | 커뮤니티 공개 배포 전 체크리스트 | 아직 수행 전 |
| [operations-console-release.md](vaildates/operations-console-release.md) | 운영 콘솔, 운영 API, Ollama | 2026-11-10. 브라우저 미검증 |
| [local-followup.md](vaildates/local-followup.md) | SMTP, 다중 챕터, 긴 입력, 회복 | 2026-11-10. 브라우저 미검증 |
| [diagnostics-integration.md](vaildates/diagnostics-integration.md) | 코드 진단 수정, 전체 자동 검사, AI 실행 계층 | 2026-11-10 |

### trainings/ — 설계 판단

| 문서 | 내용 |
|------|------|
| [2026-11-10_console-and-inference.md](trainings/2026-11-10_console-and-inference.md) | 독립 콘솔, 공통 운영 잠금, 컨텍스트를 유지하는 추론 개선 |
| [2026-11-04_review-and-moderation.md](trainings/2026-11-04_review-and-moderation.md) | 승인 시 재검증, 숨김, 정지 검사, 패키지 검사, 원본 가져오기 |
| [2026-11-03_community-design.md](trainings/2026-11-03_community-design.md) | 게시본 분리, Clone 시작 상태, 공유와 Clone의 구분 |
| [2026-11-02_chapter-content-markdown.md](trainings/2026-11-02_chapter-content-markdown.md) | 챕터 본문을 Markdown으로 저장하는 이유 |

### find_debug/ — 디버그 일지

| 문서 | 내용 |
|------|------|
| [2026-11-11](find_debug/2026-11-11.md) | 생성 품질(창작 요구 보존, 두 구간 생성, 반복 방지), 메일 전달, 동시 신고 |
| [2026-11-10](find_debug/2026-11-10.md) | 동일 작성자 일괄 조치 충돌, 공백 AI 스트림, 스트림 중단·완료 누락, AI 서버 조립 |
| [2026-11-04](find_debug/2026-11-04.md) | 빌드가 깨진 채 main까지 올라간 문제, 낡은 테스트, 편집기 빨간줄 |
| [2026-11-03](find_debug/2026-11-03.md) | 내보내기의 태그 손실, 프로필 탭 초기화, 가로 밀림 |
| [2026-10-23](find_debug/2026-10-23.md) | Java null 안전성 경고, 8000·8080 포트 충돌 |

### repeater/ — 재발 가능한 환경 이슈

| 문서 | 내용 |
|------|------|
| [local-port-conflicts.md](repeater/local-port-conflicts.md) | 5432·8000·8080 포트 충돌, 남은 백엔드 JVM |
| [browser-automation-host.md](repeater/browser-automation-host.md) | 브라우저 연결 호스트 사용 불가와 미검증 경계 |

### 그 밖

| 문서 | 내용 |
|------|------|
| [novel/continuity-rules.md](novel/continuity-rules.md) | 검사 시점, 이슈 유형, 심각도와 점수, 규칙 검사 |
| [web_management/port-inventory.md](web_management/port-inventory.md) | 로컬 개발과 배포 구성의 포트 배치 |
| [branchs/README.md](branchs/README.md) | 브랜치 목록, 커밋 기록 목록 |
| [branchs/TEMPLATE.md](branchs/TEMPLATE.md) | 커밋 기록 템플릿 |

## 어디에 써야 할지 모를 때

```
기획서 내용을 개발 스펙으로 풀어 쓴다        → plan/
서비스 구조나 상태 전이가 바뀐다             → Architecture/
"이렇게 실행하세요" 절차를 적는다            → guidance/
배포 전에 손으로 확인할 목록, 확인한 결과     → vaildates/
"왜 A 대신 B를 골랐는지" 남긴다              → trainings/
오늘 버그를 잡았다                          → find_debug/
환경 문제가 또 생길 것 같다                  → repeater/
포트, 도메인, 배포 단계 기록이다              → web_management/
Story Bible 템플릿, 프롬프트 작성 규칙이다    → novel/
커밋을 따로 기록할 필요가 있다              → branchs/commits/
보안 정책, 라이선스, 기여 안내, 변경 내역     → 루트 문서 (docs/ 밖)
서비스 실행법, 폴더 구조                     → 각 서비스의 README
```

같은 내용을 두 곳에 쓰지 않습니다. 값의 원본은 한 곳에 두고 나머지는 링크합니다. 예를 들어 포트와 환경 변수의 원본은 [CURRENT_STACK.md](plan/CURRENT_STACK.md)입니다.

## 문서를 쓰는 규칙

- **원본 우선:** 기획 원본과 모순되면 원본이 이기고 파생 문서를 고칩니다.
- **구현과 함께:** 구현을 바꾸면 [Architecture/](Architecture/README.md)와 [CURRENT_STACK.md](plan/CURRENT_STACK.md)를 같이 갱신합니다.
- **사유를 남긴다:** 기능이 바뀌거나 문제가 생겨 진행 방식을 바꾸면 사유와 바뀐 방식을 적습니다. 구조는 `Architecture/`, 스펙과 진행은 `plan/`, 선택 이유는 `trainings/`, 문제 원인은 `find_debug/`입니다.
- **확인하지 않은 것을 확인했다고 쓰지 않는다:** 자동 테스트 통과를 브라우저 검증으로, 로컬 결과를 원격 CI 통과로, mock 결과를 실제 모델 검증으로 바꿔 적지 않습니다. 못 한 것은 "미검증"으로 남깁니다.
- **넣지 않는 것:** 시크릿, 실제 사용자 데이터, 생성된 소설 원문. `trainings/`에는 코드와 프롬프트 원문도 넣지 않습니다.
- **커밋 기록:** 평소에는 쓰지 않고 필요할 때만 `branchs/commits/`에 둡니다. `docs/commits/`는 만들지 않습니다.
- **새 폴더:** 먼저 정본 [SKILL.md](../.agents/skills/storyforge/SKILL.md)의 docs 표에 용도를 추가한 뒤 만들고, 이 문서의 표도 갱신합니다.
- **README:** 필요한 곳에만 둡니다. `docs/` 바로 아래 폴더는 정의와 작성 템플릿이 README에 있어 유지하고, 그 아래 하위 폴더에는 두지 않습니다.
- **링크:** 문서를 옮기거나 이름을 바꾸면 그 문서를 가리키는 링크를 함께 고칩니다.

## 저장소의 다른 문서

| 문서 | 내용 |
|------|------|
| [README.md](../README.md) | 프로젝트 소개, 전체 스택 실행 |
| [storyforge-frontend/README.md](../storyforge-frontend/README.md) | 사용자 웹 |
| [storyforge-console/README.md](../storyforge-console/README.md) | 운영 콘솔 |
| [storyforge-backend/README.md](../storyforge-backend/README.md) | 오케스트레이터 |
| [storyforge-ai/README.md](../storyforge-ai/README.md) | AI 서비스 |
| [AGENTS.md](../AGENTS.md) / [SKILL.md](../.agents/skills/storyforge/SKILL.md) | 작업 규칙 요약 / 정본 |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | 기여 안내 |
| [SECURITY.md](../SECURITY.md) | 보안 정책, 취약점 신고, 알려진 제한, 배포 전 체크리스트 |
| [CHANGELOG.md](../CHANGELOG.md) | 변경 내역 |
| [LICENSE](../LICENSE) | 독점 (All rights reserved) |
