"""SMTP 포트에 접속만 확인합니다. 비밀번호와 메일 본문은 출력하지 않습니다."""
import os
import smtplib
import sys


def main() -> int:
    enabled = os.environ.get("MAIL_ENABLED", "false").lower() == "true"
    host = os.environ.get("SMTP_HOST", "")
    port = int(os.environ.get("SMTP_PORT", "0") or "0")
    sender = os.environ.get("MAIL_FROM", "")
    if not enabled or not host or not port or not sender:
        print("외부 메일을 보내지 않습니다. MAIL_ENABLED, SMTP_HOST, SMTP_PORT, MAIL_FROM이 필요합니다.")
        return 2
    try:
        with smtplib.SMTP(host, port, timeout=8) as smtp:
            smtp.ehlo()
            if os.environ.get("SMTP_STARTTLS", "false").lower() == "true":
                smtp.starttls()
                smtp.ehlo()
            if os.environ.get("SMTP_AUTH", "false").lower() == "true":
                user = os.environ.get("SMTP_USERNAME", "")
                password = os.environ.get("SMTP_PASSWORD", "")
                if not user or not password:
                    print("SMTP 인증 값이 비어 있습니다. 비밀번호는 출력하지 않습니다.")
                    return 1
                smtp.login(user, password)
                print(f"SMTP 인증 확인: {host}:{port}. 메일은 보내지 않았고 수신함은 확인하지 않았습니다.")
                return 0
    except OSError as exc:
        print(f"SMTP 접속 실패: {host}:{port} ({exc.__class__.__name__})")
        return 1
    except smtplib.SMTPException as exc:
        print(f"SMTP 인증 실패: {host}:{port} ({exc.__class__.__name__})")
        return 1
    print(f"SMTP 접속 확인: {host}:{port}. 수신함 도달은 확인하지 않았습니다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
