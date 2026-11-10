# 운영 콘솔 스펙

> 기준: 2026-11-10 사용자 요청. 독립 Angular 운영 사이트와 복잡한 운영 처리 지원. 상태 머신은 유지합니다.

## 화면과 권한

SMTP를 활성화한 환경은 `ADMIN_EMAILS` 일치와 이메일 인증 완료가 모두 필요합니다. 먼저 사용자 웹의 계정 설정에서 인증한 뒤 콘솔에 로그인합니다. 비밀번호 변경·재설정 후 기존 콘솔 JWT도 폐기되므로 다시 로그인해야 합니다. [계정 메일](auth.md).

루트의 `storyforge-console/`은 사용자 웹과 별도 빌드·배포합니다. 로그인만 제공하며 가입과 권한 부여 화면은 없습니다. 기존 `ADMIN_EMAILS`가 권한의 기준입니다. 로그인 응답과 `/auth/me`로 권한을 확인하고, 메뉴 이동 시 다시 확인합니다. 토큰은 탭의 `sessionStorage`에 저장하며 401 또는 로그아웃 시 지웁니다. API는 상대 경로 `/api/v1`만 사용합니다.

| 경로 | 기능 |
|------|------|
| `/login` | 관리자 로그인, 권한 오류 안내 |
| `/` | 회원·정지 회원·프로젝트·작품·대기 신고 수, 프로젝트 상태별 분포 |
| `/reports` | 상태별 신고 목록, 내용·작성자·신고자 확인, 선택 조치 |
| `/users` | 이름·이메일 검색, 정지 상태 필터, 정지·해제 |
| `/audit` | 운영 조치 이력, 운영자 ID 필터 |

목록은 페이지당 20건이며 서버는 최대 50건으로 제한합니다. 빈 목록·로딩·실패를 구분합니다. 쓰기 중에는 중복 제출을 막고, 실패 시 성공 메시지를 표시하지 않습니다. 기존 사용자 웹 `/admin`도 계속 같은 신고 API를 사용합니다.

## 외부 API

아래 경로는 `/api/v1` 기준입니다. 인증 없음 401, 일반 회원 403, 없는 대상 404, 상태 충돌 409, 입력 오류 400입니다. 서버는 서비스 진입점마다 관리자 여부를 확인합니다.

| 요청 | 입력 / 결과 |
|------|-------------|
| `GET /admin/overview` | `{users,suspendedUsers,projects,works,openReports,pipeline,capturedAt}`. 작품 수는 숨김 포함 |
| `GET /admin/users?q=&suspended=&page=&size=` | `{items,total,page,size}`. 회원은 ID·이메일·표시명·관리자·정지·사유·가입 시각. 비밀번호 해시 제외 |
| `PUT /admin/users/{id}/suspension` | `{suspended:boolean,note:string}`. 사유 1~300자, 관리자 정지 금지 |
| `GET /admin/reports?status=OPEN&page=&size=` | 기존 신고 API. 상태 `OPEN`, `RESOLVED`, `DISMISSED` |
| `PUT /admin/reports/{id}` | 기존 단건 조치 API. 신규 감사 기록에도 남음 |
| `POST /admin/reports/batch` | `{reportIds:[양의 정수],action,note}`. 최대 50건. 중복 ID 제거. `{requested,processed}` 반환 |
| `GET /admin/audit?actorId=&page=&size=` | `{items,total,page,size}`. 이력은 관리자 ID·표시명·조치·대상·신고 ID·사유·시각 |

일괄 조치는 `HIDE`, `RESTORE`, `DISMISS`, `SUSPEND`, `UNSUSPEND`를 지원합니다. 같은 대상의 여러 신고는 한 번만 조치합니다. 사유는 필수이며 대상자 알림에 사용됩니다. 단건 API의 기존 선택 사유 계약은 유지합니다.

같은 작성자의 서로 다른 대상이 포함되어도 일괄 정지·해제를 할 수 있습니다. 계정 상태 변경과 작성자 알림은 작성자당 한 번이고, 대상별 신고 처리·신고자 알림·감사 기록은 각각 실행합니다. `processed`는 서로 다른 콘텐츠·사용자 대상 수이며 계정 변경 횟수는 아닙니다. 이미 다른 요청에서 정지·해제된 계정은 기존대로 409입니다.

## 트랜잭션과 감사

`V11__operations_console.sql`이 운영 잠금 행과 `admin_audit` 및 조회 인덱스를 추가합니다. 단건·일괄·직접 회원 조치는 같은 잠금 행을 잠급니다. 이미 처리된 신고 재처리, 이미 숨긴 대상 숨김, 이미 해제된 정지 해제 등은 409입니다. 삭제된 대상에는 기각만 가능합니다.

일괄 조치 중 실패하면 앞선 변경·신고 상태·알림·감사 기록을 모두 되돌립니다. DB 트랜잭션 잠금이므로 여러 백엔드 인스턴스에서도 운영 조치 간에 적용됩니다. 감사 기록은 계정·신고가 삭제되어도 남도록 FK를 두지 않았고 수정·삭제 API는 없습니다. DB 관리자에 의한 변경까지 차단하는 별도 불변 저장소는 아닙니다. 이 기능 도입 전 조치를 소급 생성하지 않습니다.

운영 통계는 조회 시점의 집계이며 실시간 모니터링이나 AI 서버 가용성 판정이 아닙니다. 자동 정지·작품 영구 삭제·관리자 권한 변경은 제공하지 않습니다.

## 관련 문서

- [구조와 잠금 흐름](../Architecture/operations-console.md)
- [실행 및 확인](../guidance/operations-console.md)
- [검증 결과](../vaildates/operations-console-release.md)
