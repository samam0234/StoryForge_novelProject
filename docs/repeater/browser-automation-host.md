# Windows 브라우저 연결 호스트 사용 불가

## 환경과 증상

2026-11-10 Windows 데스크톱 세션에서 로컬 StoryForge 화면 검증을 시도했습니다. 브라우저 연결 API의 URL 기반 선택과 내장 브라우저 탭 생성이 모두 `The browser access host is unavailable`을 반환했습니다. 앞선 Chrome 네이티브 접근은 현재 URL을 확인할 수 없다는 안전 검사에서 중단됐습니다. 두 오류를 localhost HTTP 장애나 사이트 접근 권한 거부로 단정하지 않습니다.

## 확인한 범위

- 기존 브라우저를 URL로 연결하는 공식 API와 `iab` 내장 브라우저에 `http://localhost:4200`을 여는 공식 API가 같은 호스트 오류로 실패했습니다. 실제 페이지 렌더링이나 클릭 결과는 얻지 못했습니다.
- 설치 가능한 연결 플러그인 검색에는 클라우드 브라우저가 있었지만 로컬 루프백 연결 복구 수단은 확인되지 않았습니다. 클라우드 서비스 설치나 localhost 공개 노출을 진행하지 않았습니다.
- 저장소 코드·SMTP·DB·Ollama 설정을 바꿔 해결되는 오류라는 근거는 없습니다. 자동 테스트·빌드·HTTP 응답은 브라우저 검증으로 세지 않습니다.

## 공식 연결 경로

OpenAI 문서는 데스크톱 내장 브라우저와 localhost 화면 검증을 지원한다고 안내합니다. 사이트별 기본 권한과 예외는 **Settings → Browser → Agent permissions**에서 확인합니다. [공식 Browser 문서](https://learn.chatgpt.com/docs/browser).

공식 설정 딥 링크는 다음과 같습니다. [공식 Commands 문서](https://learn.chatgpt.com/docs/reference/commands).

- `codex://settings/browser-use`: 브라우저 설정
- `codex://settings/computer-use/google-chrome`: Chrome 컴퓨터 사용 설정

설정 화면에서 연결 상태와 로컬 사이트 권한을 확인해야 합니다. 현재 호스트 오류의 원인이 권한인지, 앱 연결 상태인지, 관리 정책인지까지는 확인되지 않았습니다. 단순히 권한을 켜면 해결된다고 보장하지 않습니다.

## 에이전트가 수행할 수 없는 부분

설치된 computer-use 스킬의 Windows 안전 지침은 `Do not automate the ChatGPT desktop app UI`와 `Do not change ... in-app security or privacy settings`를 명시합니다. 앱 자신의 설정 UI를 자동으로 조작하거나 접근 검사를 우회하는 방식으로 연결을 복구하지 않습니다. 공식 브라우저 호스트 연결이 정상화된 뒤 같은 로컬 QA 환경에서 메일 인증·운영 콘솔 검증을 다시 해야 합니다.

## 현재 결과

브라우저 검증은 미완료입니다. 후속 사용자가 코드 진단·문서 수정 후 병합·푸시를 직접 요청해 이전 보류를 변경했습니다. 이 연결 제한은 통합 뒤에도 남는 확인 대상입니다. [최신 진단·통합 검증](../vaildates/diagnostics-integration.md), [이전 검증 기록](../vaildates/local-followup.md).
