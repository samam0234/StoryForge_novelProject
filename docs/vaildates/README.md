# docs/vaildates/

배포·데모 전에 사람이 손으로 확인하는 **수동 검증 체크리스트** 폴더입니다.

> **폴더명 스펠링(`vaildates`)은 의도적입니다. 교정하지 않습니다.**

## 파일명

`대상-목적.md` (kebab-case). 예: `mvp-demo.md`, `chapter-pipeline-release.md`

## 템플릿

```markdown
# 체크리스트 제목

- 대상 버전 / 커밋:
- 확인자:
- 일시:

## 사전 조건
- [ ] 서비스 기동 확인 (포트는 ../plan/CURRENT_STACK.md)

## 파이프라인
- [ ] Story Bible 생성 → 편집 → 확정 (BIBLE_DRAFT → BIBLE_CONFIRMED)
- [ ] Outline 생성 → 편집 → 확정 (OUTLINE_DRAFT → OUTLINE_CONFIRMED)
- [ ] 챕터 생성 스트리밍 시작 (목표: 5초 이내)
- [ ] Continuity 리포트 표시
- [ ] CHAPTER_REVIEW 단계에서 승인 / 재생성 동작

## 보안·경계
- [ ] 브라우저에서 FastAPI에 직접 접근할 수 없음
- [ ] `.env` 시크릿이 저장소에 없음

## 결과
- 통과 / 실패 항목과 후속 조치
```

위 항목은 기획서 §4, §6, §7, §13을 기준으로 한 예시입니다. 대상에 맞게 고쳐 씁니다.

## 문서 목록

| 문서 | 대상 | 최근 확인일 |
|------|------|-------------|
| (아직 없음) | | |
