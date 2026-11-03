# Should 기능 스펙

> 기준: 기획서 v1.3 §6.1 (버전 관리), §6.4 (내보내기, 히스토리·Diff), §9.6-5 (프로젝트 설정)

## 내보내기

`GET /api/v1/projects/{id}/export?format=md|txt|docx|pdf|html|epub|odt|rtf` — `format`을 생략하면 프로젝트 설정값을 씁니다.

| 항목 | 결정 | 이유 |
|------|------|------|
| 대상 | **승인된 챕터만.** 없으면 409 | 검토 전 초안이 원고에 섞이지 않도록 (사용자 검토 원칙) |
| 원본 | 챕터 Markdown을 파싱해 DOCX·PDF로 변환 | [저장 형식 결정](../trainings/2026-11-02_chapter-content-markdown.md) |
| DOCX | Apache POI. 글꼴은 맑은 고딕 지정 | 없는 환경에서는 Word가 대체 글꼴을 씀 |
| PDF | OpenPDF + **나눔고딕 내장** (OFL, 약 4MB) | 내장하지 않으면 한글이 깨짐. 환경에 관계없이 같은 결과 |

제한: PDF에는 기울임 글꼴이 없어 기울임이 일반체로 나옵니다.

## 버전 이력

| 대상 | API | 저장 시점 |
|------|-----|-----------|
| Story Bible | `GET /projects/{id}/bible/versions` | 생성·저장할 때마다 전체 스냅샷 |
| 챕터 | `GET /chapters/{id}/versions` | AI 생성(`GENERATED`), 내용이 바뀐 저장(`EDITED`) |

화면의 "이력" 버튼에서 두 버전을 골라 단어 단위 Diff로 비교합니다. **불러오기는 편집기에 채우기만 하고, 저장은 사용자가 합니다.** 확정 후에는 볼 수만 있습니다. 마이그레이션 전 데이터는 현재 내용이 첫 이력이 됩니다.

## 프로젝트 설정

`PUT /api/v1/projects/{id}` — 제목, 장르, 모델, 기본 톤, 기본 내보내기 형식. 기획서 §10 Project에 `model`, `defaultTone`, `exportFormat`을 추가했습니다 (§9.6-5에 필요). 설정 화면에는 프로젝트 삭제도 있습니다.

- 모델: `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-haiku-5-5` 중 선택. 백엔드가 목록을 검사합니다. mock 모드에서는 적용되지 않습니다.
- Haiku에는 서버 측 refusal fallback이 없어 그 옵션을 빼고 호출합니다.
- 기본 톤은 Bible 생성 프롬프트에 들어갑니다. 챕터는 Bible의 톤을 따릅니다.
- **실제 모델 호출은 검증하지 못했습니다** (Claude API 연동 보류, [roadmap-progress.md](roadmap-progress.md)).
