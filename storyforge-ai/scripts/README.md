# AI 보조 실행 도구

성능 측정과 수동 검증용 도구를 둡니다. 서버 구현은 `../app/`, 자동 회귀 테스트는 `../tests/`에 둡니다. 서비스 루트에 실행 래퍼를 남기지 않습니다.

## 실행

`storyforge-ai` 폴더에서 가상환경 의존성을 설치한 뒤 모듈로 실행합니다. 아래 `python`은 Windows의 `.venv/Scripts/python`, macOS/Linux의 `.venv/bin/python`을 뜻합니다.

| 용도 | 명령 |
|---|---|
| 짧은 프롬프트 지연 측정 | `python -m scripts.benchmark_ollama --runs 3 --warmup` |
| 슬롯 포화·중단·긴 컨텍스트 회복 | `python -m scripts.validate_load` |
| 합성 작품의 다중 챕터 생성·요약·Continuity | `python -m scripts.validate_pipeline --chapters 3` |
| 반복 생성·포화·중단 후 재사용 | `python -m scripts.validate_soak --duration 2400 --timeout 180` |
| 이름 풀 Chroma 임베딩 | `python -m scripts.embed_characters` |

각 모듈의 `--help`는 모델을 호출하지 않습니다. 실제 실행에는 로컬 Ollama 모델이 필요합니다. `.env`는 서비스 루트 기준으로 읽습니다. 이름 풀은 서비스 `data/names.json`에서 찾고, 파이프라인 시험의 인물 기록은 임시 폴더에 격리합니다. `embed_characters`만 Chroma 컬렉션 `characters`에 이름 풀을 넣으며 신경망 모델을 받지 않습니다. 원고의 DB 저장·사용자 승인·브라우저 확인을 대신하지 않습니다.

통합 진입점은 `python -m scripts <도구> <인자>`입니다. 예: `python -m scripts validate_pipeline --chapters 3`. `__main__.py`가 `orchestration.py`에 위임하며 네 도구만 허용합니다. 인자와 도구의 종료 코드를 유지하고 실행 후 `sys.argv`를 복원합니다. 기존 `python -m scripts.<도구>`도 지원합니다. 파이프라인·반복 생성 도구는 서버와 같은 `AiTaskOrchestrator`를 사용합니다.

설정과 측정 한계는 [Ollama 가이드](../../docs/guidance/ollama-performance.md), 서비스 실행은 [상위 README](../README.md)를 참고합니다.
