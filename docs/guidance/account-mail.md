# SMTP 계정 메일

## 자동 로컬 검증

Docker와 JDK가 있는 환경에서 `storyforge-backend` 폴더에서 실행합니다.

```powershell
.\mvnw.cmd "-Dtest=AccountMailIntegrationTest" test
```

Testcontainers PostgreSQL과 임의 루프백 포트의 SMTP 수신기를 사용합니다. 외부 메일로 전달하지 않습니다. 실제 JavaMail 전송·MIME 디코딩 후 링크를 API로 소비하여 인증, 재설정, 세션 폐기, 재사용·만료·다른 목적 거부, 동시 소비, 재발급, SMTP 실패 후 복구를 확인합니다. 브라우저 클릭 검증과는 별개입니다.

## 수동 화면 확인 환경

루프백 SMTP 수신기(예: 별도로 준비한 Mailpit)와 테스트 DB를 준비합니다. 기존 운영 DB에 테스트 계정을 섞지 않습니다. Spring Boot 실행 셸에 환경 변수를 주입합니다.

| 변수 | 로컬 예시 | 설명 |
|---|---|---|
| `MAIL_ENABLED` | `true` | 기본 false |
| `MAIL_FROM` | `noreply@example.test` | 수신기에 표시할 발신 주소 |
| `SMTP_HOST` | `localhost` | 기본 localhost |
| `SMTP_PORT` | `1025` | 수신기 포트. 자동 테스트는 임의 포트 |
| `SMTP_AUTH` | `false` | 공급자가 인증을 요구하면 true |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | 비움 | 실제 값은 커밋 금지 |
| `SMTP_STARTTLS` | `false` | 로컬 수신기용. 외부 SMTP는 TLS와 공급자 인증 설정 필요 |
| `FRONTEND_ORIGIN` | `http://localhost:4200` | 경로·후행 슬래시 없는 출처. 로컬 이외에는 HTTPS |

연결·읽기·쓰기 제한은 각각 5초입니다. 메일 기능을 켜려면 발신 주소와 올바른 웹 출처가 필요합니다. 설정값은 [CURRENT_STACK](../plan/CURRENT_STACK.md), API는 [auth](../plan/auth.md)를 참고합니다.

1. 가입 후 계정 설정에서 인증 메일을 요청합니다.
2. 수신기의 링크를 열고 버튼으로 인증합니다. 새로고침한 계정 설정에 인증 완료가 보여야 합니다.
3. 로그아웃 후 로그인 화면의 비밀번호 재설정 링크에서 메일을 요청합니다.
4. 새 비밀번호를 입력하고 완료합니다. 이전 비밀번호와 이전 JWT는 실패하고 새 비밀번호는 로그인돼야 합니다.
5. 같은 링크 재사용, 만료, 잘못된 링크, SMTP 비활성·연결 실패, 모바일·키보드 이동을 확인합니다.

## 제한과 병합 기준

현재 실제 브라우저 검증은 미완료입니다. 2026-11-10 사용자 지시에 따라 브라우저 검증 전 `develop`·`main` 병합을 보류합니다. 실제 공급자·발신 주소 확정 및 외부 메일 수신 검증도 별도이며 이번에는 배포하지 않습니다.
