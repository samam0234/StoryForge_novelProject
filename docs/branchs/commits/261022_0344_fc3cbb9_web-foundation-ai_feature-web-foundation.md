# feat(ai): FastAPI AI 파이프라인 기초 구현

## 제목 / 커밋번호

- **제목**: `feat(ai): FastAPI AI 파이프라인 기초 구현`
- **커밋번호**: `fc3cbb9`
- **브랜치**: `feature/web-foundation`
- **일시**: 2026-10-22 03:44

## 주 내용

FastAPI로 Bible·Outline 생성, 챕터 스트리밍 생성, 요약, Continuity 검증 내부 API를 만들었습니다.

## 상세

- `core/llm.py`: mock(기본, 키 불필요)과 Claude(`claude-opus-5-5`, `fallbacks: "default"`, effort 명시) 공급자
- `prompts/*.j2`: Bible + Outline + 이전 챕터 요약 + 캐릭터 상태를 순서대로 주입
- `services/continuity_checker.py`: 기획서 §6.3 네 유형, 규칙 기반 검사 + LLM 검증, 심각도별 점수
- `api/v1`: `/internal/generate/{bible,outline,chapter}`, 챕터는 token → done | error SSE
- mock 생성물에는 mock 표시 문구를 넣어 실제 AI 결과와 구분

## 결과

- pytest 3건 통과, 모든 프롬프트 템플릿 렌더링 확인
- 실제 Claude 호출은 API 키가 없어 검증하지 못함 (클라이언트 설정만 확인)
