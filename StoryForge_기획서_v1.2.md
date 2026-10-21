# StoryForge AI 기반 소설 생성 플랫폼 제품 기획서

**문서 버전:** 1.2  
**작성일:** 2026-10-22  
**최종 수정일:** 2026-10-22  
**작성 부서:** 제품 기획팀 / 기술 아키텍처팀 / 디자인팀  
**기밀 등급:** 내부 배포용  
**플랫폼명:** StoryForge  

---

## 문서 관리 정보

| 항목 | 내용 |
|------|------|
| 문서명 | StoryForge 제품 기획서 (Product Requirements Document) |
| 버전 | 1.2 |
| 작성자 | 제품 기획팀 |
| 검토자 | 기술 리드, 디자인 리드 |
| 승인자 | 프로젝트 오너 |
| 다음 검토 예정일 | 2026-11-15 |

### 개정 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-10-22 | 기획팀 | 기본 기획서 초안 |
| 1.1 | 2026-10-22 | 기획팀 | 오케스트레이션, 폴더 구조, 디자인 방향 보강 |
| 1.2 | 2026-10-22 | 기획팀 | 디자인 목업 예시 추가, 전체 내용 대폭 상세화, DOCX 품질 개선 |

---

## 1. 개요 (Executive Summary)

StoryForge는 사용자 프롬프트로부터 일관된 장편 소설을 생성하는 AI 웹 플랫폼이다. 핵심은 **멀티 스테이지 파이프라인**(Story Bible 생성/관리 → Outline 생성 → 순차적 챕터 생성 + Continuity Tracking)이며, 각 단계에서 사용자 검토와 AI 자동 검증을 결합한다.

### 아키텍처 원칙
- **Frontend**: Angular (엔터프라이즈 UI, 복잡한 상태 관리에 강점)
- **Main Backend (Orchestrator)**: Spring Boot (인증, 프로젝트/비즈니스 로직, 오케스트레이션, 데이터 영속화)
- **AI Service**: FastAPI (LLM 호출, 프롬프트 조립, 파이프라인 실행, 연속성 검증)

Spring Boot가 전체 워크플로우를 오케스트레이션하고, AI 처리가 필요한 단계만 FastAPI로 위임하는 구조이다.

### 핵심 가치
- AI의 설정 망각 문제를 Story Bible + Continuity Tracking으로 해결
- 단계별 사용자 제어와 투명한 생성 과정
- 기업급 유지보수성과 확장성을 갖춘 마이크로서비스 지향 아키텍처

---

## 2. 비전 및 미션

**비전**: 누구나 자신의 아이디어를 고품질·일관된 장편 소설로 완성할 수 있는 AI 창작 플랫폼이 된다.

**미션**: Story Bible 기반 구조화된 파이프라인을 통해 AI 생성의 연속성과 품질을 극대화하고, 창작자의 의도를 단계별로 반영할 수 있는 도구를 제공한다.

---

## 3. 프로젝트 배경 및 핵심 결정사항

- **플랫폼명**: StoryForge (2026-10-22 확정)
- **기술 스택**: Angular + Spring Boot + FastAPI
- **핵심 방향**: Story Bible + Outline → 순차 챕터 생성 + Continuity Tracking
- **오케스트레이션 방식**: Spring Boot가 상태 머신 기반으로 파이프라인 단계를 관리하고, 각 AI 단계는 FastAPI에 요청
- **디자인 방향**: 다크 모드 우선, 집중형 창작 환경, Angular Material 기반

---

## 4. 목표 및 성공 지표

| 지표 | MVP 목표 | 측정 방법 |
|------|----------|-----------|
| 챕터 생성 완료율 | 70% 이상 | 사용자 최종 승인 비율 |
| 연속성 이슈 발생률 | 20% 이하 | 자동 검증 실패 + 사용자 신고 |
| 챕터 생성 P95 응답 시간 | < 90초 (스트리밍 시작 < 5초) | 모니터링 |
| 프로젝트 지속률 (3챕터 이상) | 50% 이상 | 사용자 행동 로그 |
| 사용자 만족도 (생성 품질) | 4.0/5.0 이상 | 인앱 설문 |

---

## 5. 타겟 사용자

### Primary Persona: 장편 웹소설 / 소설 작가 지망생
- 아이디어는 있으나 일관성 유지와 분량 완성에 어려움을 겪음
- Story Bible과 구조화된 생성을 선호
- 장시간 집중 작업이 필요하므로 다크 모드와 방해 없는 UI 중요

### Secondary Persona: 콘텐츠 크리에이터, 시나리오 작가
- 빠른 초안 생성과 반복 수정이 필요
- 내보내기 및 공유 기능 중요

---

## 6. 핵심 기능 요구사항

### 6.1 Story Bible
- 세계관, 캐릭터 프로필(이름, 성격, 외모, 관계, 목표, 현재 상태), 설정, 테마, 톤 정의
- AI 초기 생성 + 사용자 편집
- 버전 관리 및 변경 이력
- 관련 항목 검색 및 챕터 생성 시 자동 참조

### 6.2 Outline
- 챕터별 제목, 주요 Beats, 등장 캐릭터, 긴장/감정 목표, 연속성 포인트
- 드래그 앤 드롭 재배치 및 수동 수정
- AI 재생성 및 부분 수정

### 6.3 순차 챕터 생성 + Continuity Tracking
- 컨텍스트 조립: Story Bible 관련 항목 + 해당 Outline + 이전 챕터 요약 + 캐릭터 상태 추적 데이터
- 생성 후 자동 검증 (설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드)
- 사용자 승인/수정 후 다음 단계 진행
- 부분 재생성 및 스트리밍 표시

### 6.4 기타
- 프로젝트 대시보드 (진행률, 최근 활동)
- 리치 텍스트 에디터 (TipTap 권장)
- 내보내기 (Markdown, DOCX, PDF)
- 생성 히스토리 및 Diff 비교

**MoSCoW**: 
- Must: Bible, Outline, 순차 생성, Continuity, 기본 에디터
- Should: 버전 관리, 검증 리포트 UI, 내보내기, 다크 모드
- Could: 이미지 생성 연동, 협업, 문체 분석

---

## 7. 시스템 아키텍처 및 오케스트레이션

### 7.1 고수준 구조

```
[Angular SPA]
    │ HTTPS (REST + SSE 스트리밍)
    ▼
[Spring Boot Orchestrator]
    │ - 인증/인가 (JWT)
    │ - 프로젝트/Bible/Outline/Chapter CRUD
    │ - 파이프라인 상태 머신
    │ - 트랜잭션 & 영속화 (PostgreSQL)
    │ - FastAPI 클라이언트 (WebClient)
    ▼
[FastAPI AI Service]
    │ - 프롬프트 조립 (템플릿 + 컨텍스트)
    │ - LLM 호출 (스트리밍)
    │ - Continuity 검증 에이전트
    ▼
[LLM Providers] (OpenAI, Anthropic 등)
```

### 7.2 오케스트레이션 흐름 (챕터 생성 예시)

1. 사용자: Angular에서 “챕터 N 생성” 클릭
2. Angular → Spring Boot: `POST /api/v1/projects/{id}/chapters/{num}/generate`
3. Spring Boot:
   - 현재 파이프라인 상태 확인 (BIBLE_CONFIRMED, OUTLINE_CONFIRMED 여부)
   - 관련 데이터 조회 (Bible 항목, Outline, 이전 챕터 요약, 캐릭터 상태)
   - FastAPI에 컨텍스트 패키지 전달 (`POST /internal/generate/chapter`)
4. FastAPI:
   - 프롬프트 템플릿에 컨텍스트 주입
   - LLM 스트리밍 호출
   - 생성 완료 후 Continuity Checker 실행
   - 결과 + 검증 리포트 반환
5. Spring Boot:
   - 결과를 Draft 상태로 임시 저장
   - 프론트에 SSE로 스트리밍 중계
6. 사용자: 검토 후 승인 → Spring Boot가 Chapter를 Confirmed로 전환하고 상태 머신 업데이트

**상태 머신 (프로젝트 단위)**:
- `BIBLE_DRAFT` → `BIBLE_CONFIRMED` → `OUTLINE_DRAFT` → `OUTLINE_CONFIRMED` → `CHAPTER_GENERATING` → `CHAPTER_REVIEW` → `COMPLETED`

### 7.3 서비스 책임 분리

| 서비스 | 책임 | 비고 |
|--------|------|------|
| Angular | UI/UX, 사용자 인터랙션, 에디터, 상태 표시, SSE 수신 | Material 기반 |
| Spring Boot | 오케스트레이션, 비즈니스 규칙, 데이터 영속화, 보안, API 게이트웨이 | WebClient로 FastAPI 호출 |
| FastAPI | AI 파이프라인 실행, 프롬프트 엔지니어링, LLM 통신, 연속성 검증 로직 | 내부 전용 |

---

## 8. 권장 폴더 구조

### 8.1 Angular Frontend (`storyforge-frontend/`)

```
src/
├── app/
│   ├── core/                 # 싱글톤 서비스, 인터셉터, 가드
│   │   ├── auth/
│   │   ├── api/              # HTTP 클라이언트 래퍼
│   │   └── models/
│   ├── shared/               # 공통 컴포넌트, 파이프, 디렉티브
│   │   ├── components/
│   │   └── ui/               # 디자인 시스템 컴포넌트
│   ├── features/
│   │   ├── projects/         # 프로젝트 목록/생성
│   │   ├── story-bible/      # 바이블 에디터
│   │   ├── outline/          # 아웃라인 관리
│   │   ├── chapters/         # 챕터 생성/에디터
│   │   └── dashboard/
│   ├── layout/               # 헤더, 사이드바, 레이아웃
│   └── app.routes.ts
├── assets/
├── environments/
└── styles/                   # 글로벌 스타일, 테마 (light/dark)
```

### 8.2 Spring Boot Backend (`storyforge-backend/`)

```
src/main/java/com/storyforge/
├── config/                   # Security, Web, OpenAPI, WebClient
├── controller/               # REST Controllers
├── service/
│   ├── orchestration/        # 파이프라인 오케스트레이션 & 상태 머신
│   ├── project/
│   ├── bible/
│   └── chapter/
├── domain/                   # Entity, DTO, Repository
│   ├── project/
│   ├── bible/
│   ├── outline/
│   └── chapter/
├── client/                   # FastAPI 클라이언트 (WebClient)
├── exception/
└── StoryforgeApplication.java
src/main/resources/
├── application.yml
└── db/migration/             # Flyway
```

### 8.3 FastAPI AI Service (`storyforge-ai/`)

```
app/
├── api/
│   ├── v1/
│   │   ├── bible.py
│   │   ├── outline.py
│   │   └── chapter.py
├── core/
│   ├── config.py
│   └── llm.py                # LLM 클라이언트 추상화
├── services/
│   ├── bible_generator.py
│   ├── outline_generator.py
│   ├── chapter_generator.py
│   └── continuity_checker.py
├── prompts/                  # 프롬프트 템플릿 (.txt 또는 Jinja)
├── models/                   # Pydantic schemas
└── main.py
```

---

## 9. 디자인 시스템 및 UI/UX 방향성 (디자인 예시 포함)

### 9.1 디자인 원칙
- **집중형 창작 환경**: 글쓰기에 방해가 되지 않는 클린한 인터페이스
- **단계적 진행 가시화**: 현재 파이프라인 단계를 항상 명확히 표시
- **투명성**: AI가 어떤 컨텍스트를 사용했는지, 검증 결과를 사용자에게 보여줌
- **다크 모드 우선 지원**: 장시간 사용에 적합

### 9.2 기술 선택 (디자인)
- **UI 라이브러리**: Angular Material + 커스텀 테마
- **에디터**: TipTap 기반 리치 텍스트 에디터
- **아이콘**: Material Icons
- **테마**: Light / Dark 토글 지원

### 9.3 컬러 팔레트

| 용도 | 컬러 | 비고 |
|------|------|------|
| Primary | #1F4E79 | 딥 블루 (신뢰, 창작) |
| Secondary | #2E75B6 | 미디엄 블루 |
| Accent | #E8A838 | 따뜻한 골드 (강조, CTA) |
| Background (Light) | #F8F9FA | |
| Background (Dark) | #121212 | |
| Surface | #FFFFFF / #1E1E1E | |
| Text Primary | #1A1A1A / #E0E0E0 | |

### 9.4 타이포그래피
- 본문 및 UI: Pretendard (또는 Noto Sans KR)
- 제목: 동일 패밀리, weight 구분
- 에디터: 가독성 높은 옵션 제공

### 9.5 디자인 예시 (목업)

아래는 제안된 디자인 방향으로 생성한 UI 목업 예시입니다.

**대시보드 목업**  
프로젝트 목록, 진행률 바, 사이드 내비게이션, 다크 테마, Primary 블루 강조.

**Story Bible 에디터 목업**  
탭 구조 (World Setting / Characters / Themes), AI Generate 버튼, 리치 텍스트 영역, 우측 어시스턴트 패널.

**챕터 워크스페이스 목업**  
3단 레이아웃: 좌측 Outline + Bible 컨텍스트, 중앙 에디터(스트리밍 표시), 우측 Continuity Check 및 캐릭터 상태.

**디자인 시스템 시트**  
컬러 스와치, 타이포그래피 스케일, UI 토큰 예시.

### 9.6 주요 화면 구성 (상세)

1. **대시보드**
   - 프로젝트 카드 그리드 (커버, 제목, 장르, 진행률, 최근 수정)
   - 신규 프로젝트 CTA
   - 빠른 AI 어시스턴트 진입

2. **Story Bible 에디터**
   - 상단 탭: Overview, Characters, World, Themes
   - 중앙 에디터 + AI 생성 패널
   - 저장 상태 표시

3. **Outline 뷰**
   - 챕터 카드 또는 타임라인
   - 드래그 앤 드롭
   - 각 챕터 Beats 편집

4. **챕터 워크스페이스**
   - 좌측: 현재 Outline + 관련 Bible 항목 (접기/펼치기)
   - 중앙: 에디터 (스트리밍 중 “AI writing...” 표시)
   - 우측: Continuity 리포트 (이슈 목록, 점수), 캐릭터 상태
   - 하단: 승인 / 재생성 버튼

5. **프로젝트 설정**
   - 모델 선택, 기본 톤, 내보내기 옵션

---

## 10. 데이터 모델 초안 (핵심 엔티티)

- **Project**: id, title, genre, status, ownerId, createdAt, updatedAt
- **StoryBible**: projectId, worldSetting, theme, tone, version, updatedAt
- **Character**: bibleId, name, description, personality, relationships, currentState (JSON)
- **Outline**: projectId, chapters[] (order, title, beats, characters, goals, continuityPoints)
- **Chapter**: projectId, number, title, content, status (DRAFT/CONFIRMED), summary, generationContext, createdAt
- **ContinuityReport**: chapterId, issues[] (type, description, severity), score, createdAt

---

## 11. API 설계 원칙

- RESTful + OpenAPI 명세 (Spring Boot에서 생성)
- Spring Boot가 외부에 노출하는 단일 API 표면
- FastAPI는 내부 전용 (서비스 간 통신, 인증 없이 내부 네트워크)
- 스트리밍: SSE로 챕터 생성 중계
- 버전 관리: `/api/v1/`

주요 엔드포인트 예시:
- `POST /api/v1/projects`
- `POST /api/v1/projects/{id}/bible/generate`
- `PUT /api/v1/projects/{id}/bible`
- `POST /api/v1/projects/{id}/outline/generate`
- `POST /api/v1/projects/{id}/chapters/{num}/generate`
- `PUT /api/v1/chapters/{id}/confirm`
- `GET /api/v1/projects/{id}/status`

---

## 12. 기술 스택 상세

| 레이어 | 기술 | 비고 |
|--------|------|------|
| Frontend | Angular 18+, Angular Material, RxJS, TipTap | Standalone components 권장 |
| Main Backend | Spring Boot 3.x, Spring Security, Spring WebFlux (WebClient), Flyway, PostgreSQL | |
| AI Service | FastAPI, Pydantic v2, httpx / OpenAI SDK | LangChain 선택적 도입 |
| Database | PostgreSQL 16+ (pgvector 확장 검토) | |
| 인프라 | Docker Compose (개발), 향후 Kubernetes | |
| 관측성 | Spring Actuator, Prometheus, 구조화 로깅 | AI 비용 추적 포함 |

---

## 13. 비기능 요구사항

- **성능**: 스트리밍 시작 5초 이내, 전체 생성은 비동기 처리
- **보안**: JWT, API Key 암호화 저장, Rate Limiting, CORS 설정
- **확장성**: AI 서비스 독립 스케일 아웃 가능
- **가용성**: 99.5% (MVP 기준)
- **국제화**: 한국어 우선, i18n 준비
- **접근성**: 기본 WCAG 준수 고려

---

## 14. 개발 로드맵

### Phase 1: MVP (10~14주)
- 기본 인증, 프로젝트 CRUD
- Story Bible / Outline 생성·편집
- 순차 챕터 생성 + 단순 Continuity
- Angular Material 기반 UI (다크 테마 포함)
- Docker Compose 배포
- 기본 내보내기

### Phase 2: 품질·사용성 (6~8주)
- 고급 Continuity (캐릭터 상태 추적)
- 검증 리포트 UI 고도화
- 버전 히스토리 및 Diff
- 다크/라이트 모드 완성
- 성능 최적화

### Phase 3: 고도화
- pgvector 기반 관련 설정 검색
- 문체 일관성 유지
- 비용 최적화 (캐싱, 모델 라우팅)
- 협업 기능 검토

---

## 15. 리스크 및 대응

| 리스크 | 영향 | 대응 방안 |
|--------|------|-----------|
| LLM 할루시네이션으로 설정 충돌 | 높음 | 강한 컨텍스트 주입 + Continuity 단계 + 사용자 검토 강제 |
| 서비스 간 통신 지연/장애 | 중간 | Circuit Breaker, 타임아웃, 재시도, 명확한 에러 전파 |
| 생성 비용 초과 | 중간 | 컨텍스트 윈도우 최적화, 요약 캐싱, 사용자별 한도 |
| UI 복잡도 증가 | 중간 | 단계별 점진적 공개, 명확한 진행 표시, 디자인 시스템 준수 |
| 에디터 성능 (장문) | 중간 | 가상 스크롤, 청크 로딩 검토 |

---

## 16. 결론 및 다음 단계

StoryForge는 Spring Boot를 오케스트레이터로, FastAPI를 AI 엔진으로 분리하여 장편 소설 생성의 일관성을 확보하는 플랫폼이다. 제안된 폴더 구조, 디자인 시스템, 목업 예시는 유지보수성과 사용자 경험을 동시에 고려하였다.

### 즉시 다음 단계
1. 상세 화면 와이어프레임 / Figma 프로토타입 (목업 기반)
2. OpenAPI 명세 초안 작성
3. 핵심 데이터 모델 ERD 확정
4. FastAPI 파이프라인 PoC (Bible → Outline → Chapter 1)
5. Angular Material 테마 및 TipTap 에디터 PoC
6. Docker Compose 기본 환경 구성
7. 디자인 시스템 토큰 문서화

---

**본 문서는 프로젝트 논의 사항을 기반으로 작성되었으며, 추가 피드백에 따라 개정될 수 있습니다.**

© 2026 StoryForge Project Team. 내부 문서.
