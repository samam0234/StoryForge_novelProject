## 제목 / 커밋번호

- **제목**: `feat(api): 신고, 관리자 조치, 차단, 계정 정지, 이용약관, 알림 추가`
- **커밋번호**: `1c540d8`
- **브랜치**: `feature/auth`
- **일시**: 2026-11-04 20:29

## 주 내용

공개 게시물을 사후에 처리할 수단을 만듭니다. 신고, 관리자 조치, 차단, 계정 정지, 이용약관, 알림을 추가합니다.

## 상세

- V8, `domain/moderation`, `domain/notification`, `ModerationService`, `NotificationService`, `AccountPolicy`, `AccountGuardInterceptor`
- 스펙: [moderation.md](../../plan/moderation.md)

## 결과

- `ModerationIntegrationTest` 12건 통과
- 이메일 인증과 비밀번호 재설정은 메일 수단이 정해지지 않아 제외
- 약관 문구는 법률 검토 전 초안
- 이 기록은 2026-11-04에 소급 작성했습니다.
