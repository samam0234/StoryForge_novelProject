# docs/web_management/

배포 **전 / 중 / 후** 문서와 **포트 인벤토리**를 두는 폴더입니다.

## 구성

| 문서 | 내용 |
|------|------|
| [port-inventory.md](port-inventory.md) | 환경별 서비스 포트·도메인 목록. 지금은 로컬만 있음 |
| `deploy-YYYY-MM-DD.md` (아직 없음) | 배포 1회 기록: 전(준비·체크), 중(절차·로그), 후(확인·롤백 여부) |

## 배포 기록 템플릿

```markdown
# 배포 YYYY-MM-DD

## 배포 전
- 대상 커밋 / 브랜치 (main)
- 검증 체크리스트: ../vaildates/xxx.md 결과
- DB 마이그레이션(Flyway) 여부

## 배포 중
- 실행한 절차와 시각
- 이상 징후

## 배포 후
- 상태 확인 결과 (Actuator, 로그)
- 롤백 여부와 이유
```

## 원칙

- 포트의 **확정값**은 [../plan/CURRENT_STACK.md](../plan/CURRENT_STACK.md)가 원본입니다. 인벤토리는 환경별 실제 배치를 기록합니다.
- FastAPI는 내부망 전용이므로 외부 노출 포트가 없어야 합니다.
- 시크릿 값은 적지 않습니다. 보관 위치만 적습니다.
- 운영 프로세스는 `spring.profiles.active=prod`로 띄웁니다. 이 프로필은 개발용 DB 계정, 빈 JWT, CORS 주소가 하나가 아니면 시작하지 않고 API 문서를 끕니다. FastAPI(8001)는 이 인벤토리의 외부 포트에 넣지 않습니다.
