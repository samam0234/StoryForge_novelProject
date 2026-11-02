# 변경 내역

[Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 형식을 따릅니다. 아직 정식 릴리스(버전 태그)가 없어서 날짜별로 정리합니다. 진행 방식이 바뀐 사유는 [docs/plan/roadmap-progress.md](docs/plan/roadmap-progress.md)에 있습니다.

## [Unreleased]

### 2026-11-03

#### 변경
- README를 필요한 곳에만 두도록 정리 (96개 → 29개). 정본 v1.3.

#### 추가
- 저장소 문서: `LICENSE`(독점), `SECURITY.md`, `CONTRIBUTING.md`, `THIRD_PARTY_NOTICES.md`, `CHANGELOG.md`, PR 템플릿

### 2026-11-02

#### 추가
- **인증**: 가입·로그인, JWT, 프로젝트·챕터 소유자 검사 (다른 사용자의 리소스는 404)
- **TipTap 에디터**: 챕터 본문 편집. 본문은 Markdown으로 저장
- **내보내기**: 승인된 챕터를 Markdown, DOCX, PDF로 (PDF는 나눔고딕 내장)
- **버전 이력·Diff**: Story Bible 스냅샷, 챕터 생성·수정 이력, 비교와 불러오기
- **프로젝트 설정**: 제목, 장르, 모델, 기본 톤, 기본 내보내기 형식, 삭제
- **안정성**: 로그인·가입·AI 생성 요청 제한(429), Circuit Breaker, 서버 재시작 시 상태 복구, 같은 챕터 동시 생성 차단
- 통합 테스트(실제 PostgreSQL), 프론트·AI 단위 테스트

#### 변경
- 기획서 v1.3: 상태 머신 추가 전이 2개 정식 반영, Spring Boot 4.x
- Spring Boot 3.5 → 4.1 (Jackson 3, springdoc 3)
- AI 서비스 기본 포트 8000 → 8001
- 커밋 기록은 필요할 때만 작성 (정본 v1.2)

#### 수정
- 챕터 화면 하단 버튼이 잘리던 문제
- AI 호출 타임아웃이 500으로 나가던 문제 (이제 502)

### 2026-10-23

#### 수정
- 백엔드 null 안전성 경고 정리 (트랜잭션 결과가 비어 있을 수 있던 문제 포함)

#### 변경
- 서비스별 README를 실행 방법 중심으로 개편

### 2026-10-22

#### 추가
- 저장소 뼈대, 규칙 정본과 도구별 어댑터, `docs/` 체계
- 첫 구현: Story Bible → Outline → 챕터 순차 생성(스트리밍) → Continuity 리포트 → 승인, 다크/라이트 테마
- Angular, Spring Boot, FastAPI 서비스와 개발용 PostgreSQL(Docker Compose)

## 알려진 보류 항목

- 실제 Claude API 연동 검증 (현재 mock 공급자로 동작)
- 편집한 챕터를 승인할 때 요약·Continuity 재생성 (결정 대기)
