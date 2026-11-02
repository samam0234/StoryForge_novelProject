# storyforge-frontend/src/app/shared/components/rich-text-editor/

TipTap 기반 리치 텍스트 에디터 컴포넌트입니다 (기획서 §9.2). 입출력은 Markdown입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `rich-text-editor.component.ts` | 입력 `value`, `editable`, 출력 `valueChange`. 굵게·기울임·소제목·인용·목록·실행 취소 툴바 |

## 들어갈 것

- 에디터 동작과 서식 툴바

## 들어가지 않을 것

- 저장, API 호출, 스트리밍 표시 → 사용하는 화면(`features/chapters/`)

## 관련 문서

- [저장 형식 결정](../../../../../../docs/trainings/2026-11-02_chapter-content-markdown.md)
- 상위 폴더: [../README.md](../README.md)
