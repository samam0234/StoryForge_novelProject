<div align="center">

<img src="docs/images/hero.png" alt="StoryForge — 한 줄 아이디어에서 설정이 무너지지 않는 장편 소설까지" width="100%">

<br>

**Story Bible → Outline → 챕터 → Continuity 검증 → 사람의 승인**<br>
AI와 함께 쓰되, 설정은 무너지지 않고 결정은 사람이 하는 장편 소설 창작 플랫폼

<br>

[![CI](https://github.com/samam0234/StoryForge_novelProject/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/samam0234/StoryForge_novelProject/actions/workflows/ci.yml)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.158-009688?logo=fastapi&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Status](https://img.shields.io/badge/status-in_development-E8A838)
![License](https://img.shields.io/badge/license-proprietary-555)

**[영상으로 보기](#영상으로-먼저-보기)** · **[화면 둘러보기](#화면-둘러보기)** · **[아키텍처](#아키텍처)** · **[기술 스택](#기술-스택)** · **[빠른 시작](#빠른-시작)** · **[문서](#문서-지도)**

</div>

<br>

## 영상으로 먼저 보기

<table>
  <tr>
    <td width="50%" valign="top">
      <a href="docs/images/demo/tour.mp4"><img src="docs/images/demo/tour-poster.png" alt="투어 영상 보기. 누르면 MP4가 열립니다"></a>
    </td>
    <td width="50%" valign="top">
      <img src="docs/images/demo/generate.gif" alt="챕터 생성. 본문이 스트리밍으로 흘러나오고 끝나면 Continuity 결과가 붙습니다">
    </td>
  </tr>
  <tr>
    <td align="center"><b>전체 투어</b> · 10개 화면을 33초에 (MP4, 3.3MB)</td>
    <td align="center"><b>챕터 생성</b> · 쓰이는 대로 흘러나오고, 끝나면 검증 결과가 붙습니다 · <a href="docs/images/demo/generate.mp4">MP4</a></td>
  </tr>
</table>

> [!NOTE]
> 화면 속 작가, 작품, 문장은 소개를 위해 **직접 쓴 샘플 데이터**입니다. 실제 사용자와 무관하고, 언어 모델이 쓴 글도 아닙니다.
> 챕터 생성 장면은 API 키 없이 동작하는 **mock 생성기**로 찍었습니다(화면에 "개발용 mock 생성기 출력" 표시가 보입니다). 실제 모델의 문장 품질을 보여 주는 영상이 아닙니다. 촬영 방법은 [docs/images/README.md](docs/images/README.md)에 있습니다.

<details>
<summary><b>목차</b></summary>

- [영상으로 먼저 보기](#영상으로-먼저-보기)
- [무엇을 해결하나](#무엇을-해결하나)
- [화면 둘러보기](#화면-둘러보기)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [기술 스택](#기술-스택)
- [빠른 시작](#빠른-시작)
- [언어 모델 고르기](#언어-모델-고르기)
- [운영 콘솔](#운영-콘솔)
- [배포 구성](#배포-구성)
- [환경 변수](#환경-변수)
- [테스트와 검증](#테스트와-검증)
- [현재 구현 범위와 한계](#현재-구현-범위와-한계)
- [저장소 구조](#저장소-구조)
- [문서 지도](#문서-지도)
- [작업 규칙](#작업-규칙) · [보안](#보안) · [라이선스](#라이선스)

</details>

## 무엇을 해결하나

언어 모델에게 장편을 통째로 맡기면 뒤로 갈수록 **설정을 잊습니다.** 죽은 인물이 다시 나오고, 세계의 규칙이 바뀌고, 앞에서 깔아 둔 복선이 사라집니다. StoryForge는 이 문제를 세 가지로 막습니다.

| 원칙 | 방법 |
|------|------|
| **설정을 먼저 고정한다** | Story Bible과 Outline을 사람이 확정해야 챕터를 쓸 수 있습니다 |
| **매번 다시 알려 준다** | 챕터를 생성할 때마다 Bible, 해당 장의 Outline, 승인된 이전 챕터의 요약을 컨텍스트로 넣습니다 |
| **검증하고, 사람이 결정한다** | 생성 직후 Continuity 검사 결과를 보여 주고, 사용자가 승인해야 다음 챕터로 갑니다. **자동 승인은 없습니다** |

<img src="docs/images/pipeline.png" alt="창작 파이프라인. Story Bible, Outline, 챕터 생성, Continuity 검증, 사용자 승인, 게시와 공유" width="100%">

사람이 원고를 고치면 승인할 때 요약과 검증을 다시 만듭니다. 고친 내용과 어긋난 옛 요약이 다음 챕터로 넘어가지 않게 하기 위해서입니다.

프로젝트는 아래 상태를 순서대로 지납니다. 상태 전이는 백엔드의 한 곳([PipelineStateMachine](storyforge-backend/src/main/java/com/storyforge/service/orchestration/PipelineStateMachine.java))에서만 일어납니다.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> BIBLE_DRAFT
    BIBLE_DRAFT --> BIBLE_CONFIRMED: Bible 확정
    BIBLE_CONFIRMED --> OUTLINE_DRAFT: Outline 생성
    OUTLINE_DRAFT --> OUTLINE_CONFIRMED: Outline 확정
    OUTLINE_CONFIRMED --> CHAPTER_GENERATING: 챕터 생성
    CHAPTER_GENERATING --> CHAPTER_REVIEW: 생성 완료
    CHAPTER_GENERATING --> OUTLINE_CONFIRMED: 첫 챕터 실패·중단
    CHAPTER_REVIEW --> CHAPTER_GENERATING: 다음 챕터 / 재생성
    CHAPTER_REVIEW --> COMPLETED: 마지막 챕터 승인
```

**Continuity 검사**는 네 가지를 봅니다: 설정 충돌, 캐릭터 일관성, 타임라인, 플롯 스레드. 점수는 100에서 시작해 심각도(high 30, medium 12, low 4)만큼 깎습니다. 심각한 이슈가 있어도 사용자가 확인하면 진행할 수 있습니다. 판단은 사람이 합니다. 기준은 [continuity-rules.md](docs/novel/continuity-rules.md), 전이 규칙과 실패 복귀는 [pipeline-state-machine.md](docs/Architecture/pipeline-state-machine.md)에 있습니다.

---

## 화면 둘러보기

### ① 쓰기 전에 — 설정을 고정합니다

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/bible.png" alt="Story Bible의 인물 탭. 이름, 설명, 성격, 관계"></td>
    <td width="50%"><img src="docs/images/screens/outline.png" alt="Outline. 챕터별 제목, Beats, 등장 인물, 목표, 연속성 포인트"></td>
  </tr>
  <tr>
    <td><b>Story Bible</b> — 세계관, 테마, 톤, 인물을 정리하고 확정합니다. 버전 이력에서 이전 내용과 비교할 수 있습니다.</td>
    <td><b>Outline</b> — 챕터마다 Beats, 등장 인물, 감정 목표, 지켜야 할 연속성 포인트를 적습니다. 드래그로 순서를 바꿉니다.</td>
  </tr>
</table>

### ② 쓰는 동안 — 검증을 보고 사람이 승인합니다

<img src="docs/images/screens/chapter.png" alt="챕터 작업 화면. 왼쪽 챕터 목록과 Outline, 가운데 본문, 오른쪽 장면 개척과 Continuity 검사, 인물" width="100%">

| 영역 | 하는 일 |
|------|---------|
| **왼쪽** | 챕터 목록과 진행 상태, 이 장의 Outline(목표, Beats, 연속성 포인트), Bible 컨텍스트 |
| **가운데** | 스트리밍으로 생성된 본문을 TipTap 에디터에서 바로 고칩니다. 이력에서 버전을 비교합니다 |
| **오른쪽** | **Continuity 점수와 이슈.** 위 화면에서는 규칙 검사가 "Outline 인물의 전체 이름이 본문에 없다", "목표 분량보다 짧다"를 짚었습니다. 그 아래에 장면 개척과 인물 카드가 있습니다 |

승인 버튼을 눌러야 다음 장이 열립니다. 고친 본문은 승인할 때 요약과 검증을 다시 합니다.

### ③ 내 작업실

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/dashboard.png" alt="내 프로젝트. 진행 중, 완료, 게시 수와 단계별 프로젝트 목록"></td>
    <td width="50%"><img src="docs/images/screens/activity.png" alt="작업 그래프. 수동과 에이전트의 작업 비율, 날짜별 막대, 작업 기록표"></td>
  </tr>
  <tr>
    <td><b>내 프로젝트</b> — 프로젝트마다 지금 단계와 다음 할 일이 보입니다. 사람이 쓴 양과 에이전트가 쓴 양의 비율도 함께 나옵니다.</td>
    <td><b>작업 그래프</b> — 날짜별로 누가(사람/에이전트) 얼마나 썼는지 막대로 보고, 막대를 누르면 그날의 작업만 표에 남습니다.</td>
  </tr>
</table>

### ④ 함께 읽고, 이어 씁니다

<img src="docs/images/screens/board.png" alt="롱품 게시판. 표지, 장르, 요약, 태그, 추천과 Clone 수가 있는 작품 카드" width="100%">

완성한 작품을 게시하면 게시판에 올라갑니다. 프로젝트(비공개 작업실)와 작품(공개 스냅샷)은 분리되어 있어, 게시한 뒤 프로젝트를 고쳐도 게시본은 그대로입니다.

<div align="center">
  <img src="docs/images/demo/community.gif" alt="작품 화면의 탭. 읽기, 세계관, 작업 그래프, 공유 그래프, Clone 네트워크, 댓글" width="88%">
  <br><sub>작품 하나에 읽기 · 세계관 · 작업 그래프 · 공유 그래프 · Clone 네트워크 · 댓글이 함께 있습니다</sub>
</div>

<br>

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/work-share.png" alt="공유 그래프. 작가에서 채널로 이어지는 선과 방문 수 표"></td>
    <td width="50%"><img src="docs/images/screens/work-network.png" alt="Clone 네트워크. 원작에서 파생작으로 이어지는 트리와 Clone 기록"></td>
  </tr>
  <tr>
    <td><b>공유 그래프</b> — 누가 어디에 공유했고, 그 링크로 몇 명이 들어왔는지</td>
    <td><b>Clone 네트워크</b> — 이 작품을 이어 쓴 파생작의 계보. 세계관·플롯·전체 원고 중 어디까지 가져갔는지</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/screens/clone.png" alt="Clone 창. 세계관, 플롯, 전체 원고 중 범위 선택"></td>
    <td width="50%"><img src="docs/images/screens/work-graph.png" alt="공개 작품의 작업 그래프"></td>
  </tr>
  <tr>
    <td><b>Clone</b> — 범위를 골라 내 프로젝트로 가져옵니다. 원본은 바뀌지 않고, 원본 비교와 가져오기가 남습니다</td>
    <td><b>공개 작업 그래프</b> — 게시하면 요약만 공개됩니다. 본문은 공개하지 않습니다</td>
  </tr>
</table>

### ⑤ 여러 작품을 하나의 세계로 — 패키지포지

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/package.png" alt="패키지포지 편집. 독서 순서, 작품 추가, 전체 Continuity 검사 결과"></td>
    <td width="50%"><img src="docs/images/screens/package-characters.png" alt="통합 인물 사전. 여러 작품에 나온 인물과 교차 등장 표시"></td>
  </tr>
  <tr>
    <td><b>패키지 편집</b> — 작품을 유니버스·옴니버스·사가 등으로 묶고 독서 순서를 정합니다. <b>작품 사이의 Continuity 검사</b>가 같은 인물의 설명이 어긋난 곳을 짚습니다</td>
    <td><b>통합 인물 사전</b> — 묶인 작품의 인물을 한곳에 모으고, 여러 작품에 나온 인물에는 "교차 등장"을 표시합니다</td>
  </tr>
</table>

### ⑥ 커뮤니티

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/profile.png" alt="프로필. 활동 통계, 활동 잔디, 작품 목록"></td>
    <td width="50%"><img src="docs/images/screens/issues.png" alt="이슈 게시판. 달성 축하와 최근 7일 인기작"></td>
  </tr>
  <tr>
    <td><b>프로필</b> — 활동 잔디, 받은 추천과 Clone, 업적</td>
    <td><b>이슈</b> — 조회·추천·Clone 달성 소식과 최근 7일 인기작</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/screens/lounge.png" alt="창작하기. 시작 방식, 장르, 함께 창작, 창작 라운지 글 목록"></td>
    <td width="50%"><img src="docs/images/screens/guide.png" alt="AI 가이드챗. /상태 명령에 다음 할 일과 이어서 작업 버튼으로 답함"></td>
  </tr>
  <tr>
    <td><b>창작하기와 라운지</b> — 장편·단편·세계관 가져오기로 시작하고, 팁과 콜라보 모집 글을 나눕니다</td>
    <td><b>AI 가이드챗</b> — <code>/상태</code>, <code>/챕터</code>, <code>/게시</code> 같은 명령어로 다음 할 일을 안내하고 그 화면으로 보냅니다</td>
  </tr>
</table>

### ⑦ 어디서나, 밝게도

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/mobile.png" alt="휴대폰 화면. 게시판과 작품 읽기"></td>
    <td width="50%"><img src="docs/images/screens/board-light.png" alt="라이트 테마의 게시판"></td>
  </tr>
  <tr>
    <td><b>좁은 화면</b> — 390px 폭에서도 가로로 넘치지 않습니다 (수동 브라우저 점검 항목)</td>
    <td><b>라이트 테마</b> — 다크가 기본이고 상단 버튼으로 바꿉니다. 선택은 브라우저에 저장됩니다</td>
  </tr>
</table>

### ⑧ 운영 콘솔 — 운영자 전용 앱

<div align="center">
  <img src="docs/images/demo/console.gif" alt="운영 콘솔. 현황, 신고 목록, 일괄 조치, 회원, 감사 기록" width="88%">
</div>

<br>

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/console-overview.png" alt="운영 현황. 회원, 프로젝트, 작품, 대기 신고 수와 파이프라인 분포"></td>
    <td width="50%"><img src="docs/images/screens/console-reports.png" alt="신고 관리. 두 건을 골라 콘텐츠 숨김 조치와 사유 입력"></td>
  </tr>
  <tr>
    <td><b>운영 현황</b> — 회원, 프로젝트, 작품, 대기 신고와 파이프라인 단계별 분포</td>
    <td><b>신고 일괄 조치</b> — 여러 건을 골라 사유와 함께 처리합니다. <b>하나라도 실패하면 전체를 되돌립니다</b></td>
  </tr>
</table>

사용자 웹과 따로 빌드·배포하는 독립 Angular 앱입니다. 서버는 모든 운영 API에서 관리자 권한을 다시 확인하고, 조치와 알림과 감사 기록을 한 트랜잭션으로 묶습니다. 자세한 내용은 [storyforge-console/README.md](storyforge-console/README.md)에 있습니다.

<details>
<summary><b>더 많은 화면</b> — 작품 읽기, 콘솔 로그인, 회원 관리, 감사 기록</summary>
<br>

<table>
  <tr>
    <td width="50%"><img src="docs/images/screens/work.png" alt="작품 읽기. 제목, 줄거리, 통계, Clone·공유·내보내기 버튼과 챕터 본문"></td>
    <td width="50%"><img src="docs/images/screens/console-login.png" alt="운영 콘솔 로그인"></td>
  </tr>
  <tr>
    <td><b>작품 읽기</b> — 로그인 없이 읽을 수 있고, Clone·공유·내보내기(8종)를 고릅니다</td>
    <td><b>콘솔 로그인</b> — 관리자로 지정된 기존 계정만 들어갑니다. 가입 화면은 없습니다</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/screens/console-users.png" alt="운영 콘솔 회원 관리. 검색, 정지 상태 필터, 정지와 해제"></td>
    <td width="50%"><img src="docs/images/screens/console-audit.png" alt="운영 콘솔 감사 기록. 누가 언제 어떤 조치를 했는지"></td>
  </tr>
  <tr>
    <td><b>회원 관리</b> — 이름·이메일 검색, 정지 상태 필터, 사유를 남기는 정지·해제</td>
    <td><b>감사 기록</b> — 모든 운영 조치가 조치와 같은 트랜잭션에 남습니다. 수정·삭제 API는 없습니다</td>
  </tr>
</table>

</details>

---

## 주요 기능

### 창작

| 기능 | 내용 |
|------|------|
| **스트리밍 챕터 생성** | 본문이 쓰이는 대로 화면에 흘러나옵니다. 사건(Beat)이 3개 이상인 챕터는 두 구간으로 나눠 이어 씁니다 |
| **TipTap 에디터** | 생성된 원고를 바로 고칩니다. 본문은 Markdown으로 저장합니다 |
| **버전 이력과 Diff** | Bible과 챕터의 이전 버전을 단어 단위로 비교하고 되돌립니다 |
| **장면 개척** | 직전 문장에서 남길 사실과 이어 쓸 첫 문단을 제안합니다. 저장이나 승인은 하지 않습니다 |
| **반복 방지** | 같은 문장이 되풀이되는 생성은 스트리밍 중에 감지해 중단합니다 |
| **인물 이름 관리** | 이름 풀 225개에서 고르고, 다른 작품에 쓴 인물과 비슷하면 피하도록 안내합니다 |
| **함께 창작** | 초대를 수락한 사람과 원고를 공유하고, 작업 커밋으로 고친 구간을 남깁니다 |
| **AI 가이드챗** | `/상태`, `/챕터`, `/게시` 같은 명령어로 다음 할 일을 안내하고 화면으로 이동합니다 |

### 커뮤니티

스펙은 [community.md](docs/plan/community.md)에 있습니다.

| 기능 | 내용 |
|------|------|
| **게시판** | 롱품(장편), 숏품(단편), 패키지포지, 창작하기(라운지 포함), 이슈. 읽기는 로그인 없이 가능 |
| **Clone** | 세계관만, 플롯까지, 전체 원고 중 범위를 골라 내 프로젝트로 가져와 이어 씁니다. 원본 비교와 파생작 트리 |
| **작업 그래프** | 사람이 쓴 양과 에이전트가 쓴 양의 비율, 날짜별 막대, git 형식 작업 기록표 |
| **공유 그래프** | 누가 어디에 공유했고 그 링크로 몇 명이 들어왔는지 |
| **패키지포지** | 작품을 유니버스, 옴니버스, 사가 등으로 묶습니다. 연결 지도, 통합 인물 사전, 통합 내보내기, 작품 사이의 Continuity 검사 |
| **프로필** | 활동 잔디, 활동 내역, 업적 |

### 계정과 운영

| 기능 | 내용 |
|------|------|
| **인증** | JWT 액세스 토큰과 갱신 토큰. 비밀번호를 바꾸면 이전 세션은 모두 끊깁니다 |
| **계정 메일** | SMTP로 이메일 인증과 비밀번호 재설정 (기본 비활성) |
| **신고와 조치** | 신고, 숨김·복원, 계정 정지, 차단. 숨김은 삭제가 아니라 복원할 수 있습니다 |
| **운영 콘솔** | 독립 Angular 앱. 운영 집계, 회원 검색·정지, 신고 일괄 조치, 감사 기록 |
| **이용약관과 알림** | 가입 시 동의, 약관이 바뀌면 게시 전 재동의. 댓글·Clone·조치 등 알림 |
| **보호 장치** | 요청 제한(429), Circuit Breaker, 멈춘 생성 자동 복구, 운영 프로필의 시작 전 검사 |

## 아키텍처

<img src="docs/images/architecture-logos.png" alt="아키텍처. 사용자 웹과 운영 콘솔은 Spring Boot만 호출하고, Spring Boot가 PostgreSQL과 내부망의 FastAPI를 부르며, FastAPI가 언어 모델을 호출합니다" width="100%">

| 서비스 | 맡는 일 | 맡지 않는 일 |
|--------|---------|--------------|
| **사용자 웹** ([storyforge-frontend](storyforge-frontend/README.md)) | 화면, 에디터, 스트림 수신, 상태 표시 | LLM 호출, 상태 판단 |
| **운영 콘솔** ([storyforge-console](storyforge-console/README.md)) | 운영 집계, 회원·신고 조치, 감사 조회 | 창작 화면, 가입 |
| **오케스트레이터** ([storyforge-backend](storyforge-backend/README.md)) | 인증, 상태 머신, 저장, 권한, AI 중계, 내보내기 | 프롬프트 조립, LLM 호출 |
| **AI 서비스** ([storyforge-ai](storyforge-ai/README.md)) | 프롬프트 조립, LLM 스트리밍, 요약, Continuity 검증 | 인증, DB 저장, 상태 전이 |

지켜야 하는 경계는 세 가지입니다.

1. **브라우저는 Spring Boot만 호출합니다.** Angular는 FastAPI 주소를 알지 못합니다.
2. **FastAPI는 내부망 전용입니다.** 인증이 없으므로 `127.0.0.1`에만 묶고 포트를 밖에 열지 않습니다.
3. **상태 전이와 저장은 Spring Boot만 합니다.** AI 서비스는 결과를 돌려줄 뿐 무엇도 확정하지 않습니다.

통신 경로는 용도에 따라 다릅니다.

| 경로 | 방식 | 쓰는 곳 |
|------|------|---------|
| 브라우저 → Spring Boot | REST | 대부분의 요청 |
| 브라우저 → Spring Boot | SSE (`fetch` 스트림) | 챕터 본문 생성 |
| 브라우저 → Spring Boot | WebSocket `/api/v1/ws` | 가이드챗, 장면 개척 |
| Spring Boot → FastAPI | 내부 HTTP, SSE, `/internal/ws` | 생성, 검증, 실시간 중계 |
| Spring Boot → PostgreSQL | JDBC (JPA, Flyway) | 저장 |

상세한 흐름은 [system-overview.md](docs/Architecture/system-overview.md)에 있습니다.

## 기술 스택

<img src="docs/images/stack.png" alt="기술 스택. 화면 Angular·TypeScript·RxJS·Material·Vitest, 오케스트레이션 Spring Boot·Java·Flyway·Maven·JUnit, AI FastAPI·Python·Pydantic·Jinja2·Ollama·Claude, 데이터와 운영 PostgreSQL·Docker·GitHub Actions·Node.js" width="100%">

| 영역 | 기술 |
|------|------|
| 사용자 웹 | Angular 21.2 (standalone, signals, zoneless), Angular Material, TipTap 3, RxJS 7.8, TypeScript 5.9, Vitest |
| 운영 콘솔 | Angular 21.2, RxJS 7.8, TypeScript 5.9, Vitest |
| 백엔드 | Spring Boot 4.1.2 (Java 21), Spring Security(JWT), WebSocket, JPA, Flyway, Resilience4j, springdoc-openapi |
| AI 서비스 | FastAPI, Pydantic 2, Jinja2, anthropic SDK, httpx, ChromaDB, Pyright |
| 데이터 | PostgreSQL 16 |
| 인프라 | Docker Compose, Maven Wrapper, GitHub Actions |

버전과 포트의 기준값은 [CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)입니다. 바꿀 때는 그 문서를 먼저 고칩니다.

로고는 [Simple Icons](https://simpleicons.org/)(CC0)의 SVG이며 각 상표는 소유자에게 있습니다. 위치와 출처는 [docs/images/README.md](docs/images/README.md)에 있습니다.

---

## 빠른 시작

### 1. 준비물

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| Docker Desktop | 실행 중일 것 | `docker info` |
| Node.js | 24.14 이상 | `node -v` |
| JDK | 21 이상 | `java -version` |
| Python | 3.10 이상 | `python --version` |

Maven과 Angular CLI는 따로 설치하지 않습니다 (Maven Wrapper, `npx` 사용).

### 2. 실행

터미널 4개를 열고 저장소 루트에서 순서대로 실행합니다.

```bash
# ① DB — PostgreSQL 16 (호스트 포트 5433)
docker compose up -d

# ② AI 서비스 — http://127.0.0.1:8001 (처음 한 번만 venv 생성·설치)
cd storyforge-ai
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
.venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001

# ③ 백엔드 — http://localhost:8080 (Flyway가 스키마를 자동 생성)
cd storyforge-backend
./mvnw spring-boot:run

# ④ 사용자 웹 — http://localhost:4200 (처음 한 번만 npm install)
cd storyforge-frontend
npm install
npx ng serve
```

> macOS/Linux에서는 `.venv/Scripts/python` 대신 `.venv/bin/python`을 씁니다.
> Windows PowerShell·cmd에서는 `./mvnw` 대신 `.\mvnw.cmd`를 씁니다.

### 3. 동작 확인

| 확인 | 명령 / 주소 | 기대 결과 |
|------|-------------|-----------|
| AI 서비스 | `curl http://127.0.0.1:8001/health` | `{"status":"ok","provider":"mock",...}` |
| 백엔드 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger UI |
| 웹 화면 | http://localhost:4200 | 로그인 화면 |

`health`가 JSON이 아니거나 포트 사용 중 오류가 나면 [local-port-conflicts.md](docs/repeater/local-port-conflicts.md)를 보세요.

### 4. 첫 작품 만들기

1. **회원가입** — 이용약관에 동의하고 가입합니다.
2. **새 프로젝트** — 제목과 장르, 장편·단편을 고릅니다.
3. **Story Bible** — 아이디어를 적고 생성한 뒤, 고쳐서 **확정**합니다.
4. **Outline** — 챕터 수를 정해 생성하고, 순서와 내용을 다듬어 **확정**합니다.
5. **챕터** — 생성을 누르면 본문이 흘러나오고 오른쪽에 Continuity 점수가 나옵니다. 확인하고 **승인**하면 다음 챕터를 쓸 수 있습니다.
6. **게시** — 승인된 챕터를 작품으로 올리면 게시판에 나옵니다.

기본 mock 생성기의 출력에는 "※ 개발용 mock 생성기 출력입니다" 표시가 붙습니다. 파이프라인 흐름을 확인하는 용도입니다.

### 5. 종료와 초기화

| 하려는 일 | 명령 |
|-----------|------|
| 서비스 종료 | 각 터미널에서 `Ctrl+C` |
| DB 컨테이너 종료 (데이터 유지) | `docker compose down` |
| DB 데이터까지 삭제 | `docker compose down -v` |

더 자세한 절차와 문제 해결은 [local-dev-setup.md](docs/guidance/local-dev-setup.md)에 있습니다.

## 언어 모델 고르기

AI 서비스의 `LLM_PROVIDER`로 정합니다. 백엔드에도 같은 값을 주어야 "원고가 외부로 전송된다"는 안내가 맞게 표시됩니다.

| 값 | 쓰는 모델 | 필요한 것 | 용도 |
|----|-----------|-----------|------|
| `mock` (기본) | 결정적 가짜 생성기 | 없음 | 개발, 자동 테스트 |
| `ollama` | 로컬 `gemma4:e4b`. 프로젝트가 `claude-*`를 지정하면 Claude로 라우팅 | 로컬 Ollama | 일반 출력. 원고가 PC를 벗어나지 않음 |
| `anthropic` | Claude (기본 `claude-opus-5-5`) | `ANTHROPIC_API_KEY` | 강력 출력. **원고가 외부 제공자에게 전송됩니다** |

```bash
# Ollama
LLM_PROVIDER=ollama .venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001

# Claude
LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... \
  .venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001
```

PowerShell에서는 `$env:LLM_PROVIDER="ollama"`처럼 먼저 설정합니다. Ollama의 모델 상주, 예열, 동시 추론 제한과 실측 결과는 [ollama-performance.md](docs/guidance/ollama-performance.md)에 있습니다.

## 운영 콘솔

운영자 전용 독립 앱입니다. 사용자 웹과 따로 빌드하고 같은 백엔드를 씁니다.

```bash
# 백엔드를 ADMIN_EMAILS=운영자@이메일 로 실행한 뒤
cd storyforge-console
npm ci
npm start          # http://localhost:4201
```

`ADMIN_EMAILS`에 적힌 이메일의 **기존 계정**만 로그인할 수 있습니다. 메일 기능을 켠 환경에서는 그 계정의 이메일 인증까지 끝나야 관리자 권한이 생깁니다. 절차는 [operations-console.md](docs/guidance/operations-console.md)에 있습니다.

## 배포 구성

[docker-compose.deploy.yml](docker-compose.deploy.yml)이 PostgreSQL, AI, 백엔드, 사용자 웹을 한 네트워크에 올립니다. 개발용 `docker-compose.yml`(DB만)과는 별개입니다.

| 서비스 | 호스트 게시 | 비고 |
|--------|-------------|------|
| frontend | `${FRONTEND_PORT:-80}` | nginx가 `/api`와 웹소켓을 백엔드로 넘김 |
| backend | `${BACKEND_PORT:-8080}` | `JWT_SECRET`, `FRONTEND_ORIGIN`, `CONSOLE_ORIGIN` 필수 |
| ai | 없음 | 컨테이너 네트워크 안에서만 8001 |
| postgres | 없음 | `DB_NAME`, `DB_USER`, `DB_PASSWORD` 필수 |

이 구성은 **파일만 있고 실제 서버에 배포해 검증한 적은 없습니다.** 운영 콘솔은 포함하지 않으며, 도메인과 TLS는 앞단 프록시에서 정해야 합니다. 운영 전에 [SECURITY.md](SECURITY.md)의 배포 전 체크리스트와 [port-inventory.md](docs/web_management/port-inventory.md)를 확인하세요.

## 환경 변수

키 목록은 [.env.example](.env.example), 기준값과 설명 전체는 [CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)에 있습니다. 로컬 개발은 아무 설정 없이 기본값으로 실행됩니다. 자주 쓰는 값만 추리면 다음과 같습니다.

| 변수 | 서비스 | 기본값 | 설명 |
|------|--------|--------|------|
| `LLM_PROVIDER` | AI, 백엔드 | `mock` | 언어 모델 공급자 |
| `ANTHROPIC_API_KEY` | AI | (없음) | Claude를 쓸 때 |
| `JWT_SECRET` | 백엔드 | (없음) | 32바이트 이상. 비우면 시작할 때마다 새로 만들어 로그인이 풀립니다. **운영 필수** |
| `ADMIN_EMAILS` | 백엔드 | (없음) | 관리자 이메일. 쉼표로 구분 |
| `MAIL_ENABLED` | 백엔드 | `false` | 이메일 인증·비밀번호 재설정 |
| `FRONTEND_ORIGIN` / `CONSOLE_ORIGIN` | 백엔드 | `localhost:4200` / `4201` | CORS 허용 출처. 운영은 HTTPS 출처 하나씩 |
| `DB_PASSWORD` | Compose, 백엔드 | `storyforge` | 개발용. 운영에서는 반드시 바꿉니다 |

- Compose와 FastAPI는 루트 `.env`를 읽습니다. **Spring Boot는 `.env`를 자동으로 읽지 않습니다.** 실행 셸이나 배포 도구로 주입하거나, Windows에서는 `storyforge-backend/scripts/start-local.ps1`을 씁니다.
- `.env`는 커밋하지 않습니다.

## 테스트와 검증

| 서비스 | 명령 (각 서비스 폴더에서) | 건수 |
|--------|---------------------------|------|
| 백엔드 | `./mvnw test` — 통합 테스트는 Docker로 실제 PostgreSQL을 띄웁니다 | 108 |
| AI 서비스 | `.venv/Scripts/python -m pytest -q`, `.venv/Scripts/pyright --project ..` | 77 |
| 사용자 웹 | `npx ng test --watch=false`, **`npx ng build`** | 56 |
| 운영 콘솔 | `npm test`, `npm run build` | 8 |

- 건수는 2026-11-15 로컬 실행 기준입니다. 백엔드는 Docker가 꺼져 있으면 통합 테스트가 실패가 아니라 **건너뜀**으로 표시되므로 결과의 Skipped를 확인하세요.
- 사용자 웹은 **빌드까지** 확인합니다. 단위 테스트는 spec이 가져오는 파일만 컴파일하므로 화면 템플릿 오류를 놓칩니다. 실제로 테스트는 통과한 채 빌드가 깨져 `main`까지 올라간 적이 있습니다 ([2026-11-04 일지](docs/find_debug/2026-11-04.md)).
- AI 테스트는 mock과 가짜 LLM만 씁니다. 실제 모델을 호출하지 않습니다.
- 브라우저 점검은 전체 스택과 실제 브라우저가 필요해 CI에 넣지 않았습니다. 수동 도구는 [browser-check.mjs](storyforge-frontend/scripts/browser-check.mjs)이고 실행법은 [사용자 웹 README](storyforge-frontend/README.md#브라우저-점검-수동)에 있습니다.
- GitHub Actions([ci.yml](.github/workflows/ci.yml))가 `develop`, `main`으로 가는 푸시와 PR에서 네 서비스의 테스트, 두 Angular 빌드, AI 타입 검사를 실행합니다.

실제 모델과 수동 확인 결과는 [docs/vaildates/](docs/vaildates/README.md)에 날짜별로 있습니다.

## 현재 구현 범위와 한계

| 상태 | 항목 |
|------|------|
| ✅ 창작 | 인증, 프로젝트, Story Bible, Outline, 챕터 순차 생성(스트리밍), 에디터, Continuity 리포트, 고친 원고 재검증, 사용자 승인, 버전 이력·Diff, 장면 개척, 함께 창작, 내보내기 8종 |
| ✅ 커뮤니티 | 게시판 5종, 게시, 작업 그래프, Clone 3범위, 원본 비교·가져오기, 공유 그래프, 패키지포지와 전체 검사, 이슈, 프로필, 활동 잔디, 업적, 가이드챗 |
| ✅ 운영 | 독립 콘솔, 운영 집계, 회원 검색·정지, 원자적 일괄 조치, 감사 기록, 신고, 숨김·복원, 차단, 이용약관, 알림, 요청 제한, Circuit Breaker, 갱신 토큰, 계정 메일 |
| ⏸ 보류 | 실제 Claude API 검증, Codicomperchat 연동(규격 필요) |
| ⚠️ 공개 전 필요 | 이용약관 법률 검토, 실제 SMTP 공급자와 수신 확인, 실제 서버 배포 검증 |

**확인하지 못한 것**을 숨기지 않고 적습니다.

- **실제 Claude 호출:** 한 번도 검증하지 못했습니다. 자동 테스트는 가짜 LLM을 씁니다.
- **원고 분량:** 실제 Gemma로 40분 동안 95회를 생성했을 때 목표 분량(4,000~6,000자)을 채운 것은 27회였습니다. 짧은 결과는 검토 화면에 낮은 심각도 이슈로 표시되며 자동으로 거부되거나 승인되지 않습니다.
- **브라우저 확인:** 2026-11-15에 수동 점검 34건(가입, 프로젝트 화면, 게시·읽기, 로그아웃 상태, 좁은 화면, 운영 콘솔 로그인과 화면)이 통과했습니다. **메일 인증·재설정 화면, 콘솔의 신고 일괄 조치, 화면에서의 챕터 생성·Clone·패키지 흐름은 아직 브라우저로 확인하지 못했습니다.** [점검 기록](docs/vaildates/browser-check-2026-11-15.md)
- **외부 메일:** Gmail SMTP 접속과 STARTTLS는 됐지만 인증이 거절됐습니다(535). 수신함 도달은 확인하지 못했습니다.
- **내보낸 파일:** EPUB, ODT, RTF를 실제 뷰어에서 열어 본 기록이 없습니다. 자동 테스트는 파일 구조까지만 봅니다.

진행 순서와 결정 이력은 [roadmap-progress.md](docs/plan/roadmap-progress.md)에 있습니다.

## 저장소 구조

```
StoryForge/
├── storyforge-frontend/        # 사용자 웹 (Angular SPA)
├── storyforge-console/         # 운영 콘솔 (독립 Angular 앱)
├── storyforge-backend/         # 오케스트레이터 (Spring Boot)
├── storyforge-ai/              # AI 서비스 (FastAPI)
├── docs/                       # 스펙, 아키텍처, 가이드, 검증, 커밋 기록
│   ├── images/                 # README의 화면, GIF, 영상, 로고, 다이어그램
│   └── plan/Proposal/          # 기획 원본, 제3자 라이선스 고지
├── docker-compose.yml          # 개발용 PostgreSQL
├── docker-compose.deploy.yml   # 배포용 전체 구성
├── .env.example                # 환경 변수 키 목록
├── pyrightconfig.json          # Python 타입 검사 설정
├── .github/                    # CI 워크플로, PR 템플릿
├── .agents/                    # 에이전트 작업 규칙 정본
├── .claude/ .cursor/ .codex/ … # 도구별 규칙 어댑터 (정본을 가리킴)
├── AGENTS.md                   # 작업 규칙 요약
├── CONTRIBUTING.md             # 기여 안내
├── SECURITY.md                 # 보안 정책, 알려진 제한, 배포 전 체크리스트
├── CHANGELOG.md                # 변경 내역
└── LICENSE                     # 독점 (All rights reserved)
```

README는 서비스 루트와 핵심 폴더에만 둡니다. 그 밖의 폴더 설명은 가장 가까운 상위 README의 파일 표에 있습니다.

## 문서 지도

| 알고 싶은 것 | 문서 |
|--------------|------|
| 왜 이렇게 만들었나 | [기획서](docs/plan/Proposal/StoryForge_기획서_v1.2.md), [설계 판단](docs/trainings/README.md) |
| 어떻게 실행하나 | [로컬 실행](docs/guidance/local-dev-setup.md), [운영 콘솔](docs/guidance/operations-console.md), [계정 메일](docs/guidance/account-mail.md), [Ollama](docs/guidance/ollama-performance.md) |
| 구조가 어떻게 되나 | [시스템 개요](docs/Architecture/system-overview.md), [상태 머신](docs/Architecture/pipeline-state-machine.md), [운영 콘솔 구조](docs/Architecture/operations-console.md) |
| API가 무엇인가 | [api-v1.md](docs/plan/api-v1.md), [인증](docs/plan/auth.md) |
| 기능 스펙 | [커뮤니티](docs/plan/community.md), [신고·관리](docs/plan/moderation.md), [운영 콘솔](docs/plan/operations-console.md), [안정성](docs/plan/stability.md) |
| 버전과 포트, 환경 변수 | [CURRENT_STACK.md](docs/plan/CURRENT_STACK.md) |
| 무엇이 검증됐나 | [검증 기록](docs/vaildates/README.md), [진행 현황](docs/plan/roadmap-progress.md) |
| 문제가 생겼을 때 | [디버그 일지](docs/find_debug/README.md), [재발 이슈](docs/repeater/README.md) |
| README의 이미지와 영상 | [docs/images/README.md](docs/images/README.md) — 무엇을 어떻게 찍었고 다시 만드는 법 |
| 문서 전체 안내 | [docs/README.md](docs/README.md) |

## 작업 규칙

1. 지정된 작업 브랜치(현재 `feature/auth`)에서 작업합니다. 브랜치를 임의로 만들지 않습니다.
2. 커밋 메시지는 `type(scope): 한국어 요약` 형식입니다.
3. 병합은 `git merge --no-ff`만 쓰고, 흐름은 `feature → develop → main`입니다. `main` 반영은 사용자의 직접 명령이 있을 때만 합니다.
4. 기획 원본의 설계 의도, 파이프라인 단계, 상태 머신을 임의로 바꾸지 않습니다.
5. 프론트를 바꾸면 테스트와 빌드를 모두 확인합니다.

세부 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md), 요약 [AGENTS.md](AGENTS.md), 정본 [SKILL.md](.agents/skills/storyforge/SKILL.md)에 있습니다.

## 보안

취약점은 **공개 이슈로 올리지 말고** [SECURITY.md](SECURITY.md)의 절차대로 비공개로 신고해 주세요. 운영하기 전에 같은 문서의 "알려진 제한"과 "배포 전 체크리스트"를 확인하세요.

- `JWT_SECRET`과 DB 비밀번호를 개발 기본값으로 운영하면 안 됩니다. 운영 프로필(`prod`)은 이 값들이 빠지면 시작하지 않습니다.
- `LLM_PROVIDER=anthropic`이면 사용자의 소설 내용이 외부 LLM 제공자에게 전송됩니다.
- 이메일 인증을 켜지 않은 환경에서는 `ADMIN_EMAILS`의 주소로 먼저 가입한 사람이 관리자가 됩니다. 운영 전에 관리자 계정을 먼저 만드세요.

## 라이선스

Copyright © 2026 StoryForge Project Team. **All rights reserved.** 서면 허락 없이 사용·복사·수정·배포할 수 없습니다. 자세한 내용은 [LICENSE](LICENSE)를, 사용 중인 오픈소스와 글꼴의 라이선스는 [THIRD_PARTY_NOTICES.md](docs/plan/Proposal/THIRD_PARTY_NOTICES.md)를 보세요.
