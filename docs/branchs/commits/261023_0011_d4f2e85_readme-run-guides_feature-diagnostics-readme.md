# docs(docs): 서비스 README를 실행 방법 중심으로 개편

## 제목 / 커밋번호

- **제목**: `docs(docs): 서비스 README를 실행 방법 중심으로 개편`
- **커밋번호**: `d4f2e85`
- **브랜치**: `feature/diagnostics-readme`
- **일시**: 2026-10-23 00:11

## 주 내용

각 파트 메인 README를 실행 방법 중심으로 다시 쓰고, 하위 README를 실제 코드에 맞게 갱신했습니다.

## 상세

- 루트 README: 전체 스택 빠른 실행(준비물 → 실행 순서 → 확인 → Claude 사용 → 종료·초기화), 서비스별 안내, 테스트, 구현 범위
- frontend/backend/ai README: 준비물, 처음 한 번, 실행, 확인, 명령어 표, 환경 변수 표, 구조, 문제 해결, 아직 없는 것
- 코드 폴더 README 33개에 '현재 파일' 표 추가, '(예정)' 표시 제거, `core/auth`·`service` README 상태 문구 갱신
- `docs/find_debug/2026-10-23.md`, `docs/repeater/local-port-conflicts.md` 추가 및 색인·가이드 링크

## 결과

- README 누락 0, 문서 상대 링크 깨짐 0
- README에 적은 명령·경로(빌드 출력, `/docs`, `npm run watch` 등)를 실제로 확인
