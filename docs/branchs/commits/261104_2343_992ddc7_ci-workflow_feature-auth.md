## 제목 / 커밋번호

- **제목**: `ci(docs): GitHub Actions로 세 서비스의 테스트와 ng build 실행`
- **커밋번호**: `992ddc7`
- **브랜치**: `feature/auth`
- **일시**: 2026-11-04 23:43

## 주 내용

develop, main으로 가는 푸시와 PR에서 테스트와 ng build를 자동으로 돌리는 GitHub Actions를 추가합니다. 단위 테스트가 통과한 채 빌드가 깨져 main까지 올라간 일을 막기 위함입니다.

## 상세

- `.github/workflows/ci.yml` 추가
- 실제 LLM은 호출하지 않고 시크릿이 필요 없음
- 이후 작업에서 AI 타입 검사와 운영 콘솔 작업이 더해짐

## 결과

- 2026-11-15 `develop`(`fa98aa0`)과 `main`(`00d01b0`) 푸시에서 네 작업이 모두 성공
- 이 기록은 2026-11-15에 작성했습니다.
