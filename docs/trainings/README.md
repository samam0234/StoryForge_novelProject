# docs/trainings/

설계 판단과 연구 노트를 두는 폴더입니다. **"왜 그렇게 정했는지"만** 씁니다.

## 넣지 않는 것

- 코드
- 프롬프트 원문 (프롬프트 작성 규칙은 [../novel/](../novel/), 템플릿 자체는 `storyforge-ai/app/prompts/`)
- 실유저 데이터
- 생성된 소설 원문

## 파일명

`YYYY-MM-DD_주제.md`. 예: `2026-10-22_spring-orchestrator-vs-fastapi-only.md`

## 템플릿

```markdown
# 주제

- 일자:
- 관련 기획서 절:

## 질문
무엇을 결정해야 했는가

## 선택지
- A:
- B:

## 결정과 이유
무엇을 골랐고 왜 골랐는가

## 버린 선택지와 이유

## 다시 검토할 조건
어떤 상황이 되면 이 결정을 다시 볼지
```

## 문서 목록

| 문서 | 결정 |
|------|------|
| [2026-11-04_review-and-moderation.md](2026-11-04_review-and-moderation.md) | 승인 시 재검증, 숨김, 정지 검사, 패키지 검사, 원본 가져오기 |
| [2026-11-03_community-design.md](2026-11-03_community-design.md) | 게시본 분리, Clone 시작 상태, 공유와 Clone의 구분 |
| [2026-11-02_chapter-content-markdown.md](2026-11-02_chapter-content-markdown.md) | 챕터 본문 저장 형식은 Markdown |
