# Gemma 응답 지연 조정

## 전제

Ollama에 `gemma4:e4b`가 설치되어 있어야 합니다. Python 의존성은 `storyforge-ai/requirements.txt`로 설치합니다. Ollama와 FastAPI는 외부에 직접 열지 않고 내부망에 둡니다.

## 설정과 실행

`storyforge-ai` 폴더의 PowerShell에서 실행합니다. 전체 변수와 기본값은 [CURRENT_STACK](../plan/CURRENT_STACK.md)과 [.env.example](../../.env.example)에 있습니다.

```powershell
$env:LLM_PROVIDER = 'ollama'
$env:OLLAMA_MODEL = 'gemma4:e4b'
$env:OLLAMA_KEEP_ALIVE = '15m'
$env:OLLAMA_NUM_CTX = '32768'
$env:OLLAMA_THINK = 'false'
$env:OLLAMA_WARMUP = 'true'
$env:OLLAMA_MAX_CONCURRENT = '1'
.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

시작 예열은 선택이며 기본값은 `false`입니다. 예열 실패는 로그에 남기고 앱은 시작합니다. `/health`는 프로세스 상태와 설정을 보여 주며 모델의 응답 가능 여부를 보장하지 않습니다.

연결 풀을 계속 사용하고 모델을 15분간 유지합니다. `think=false`는 응답 전 추론 출력을 끕니다. 구조화 요청은 JSON 스키마를 보내고 Pydantic으로 검증합니다. 필수 창작 컨텍스트를 삭제하거나 Continuity를 생략하지 않습니다. 출력 상한은 잘림 방지 수단이 아니며 한도에 도달하면 오류로 처리합니다.

완료 신호가 있어도 공백·줄바꿈뿐인 스트림은 빈 응답 오류입니다. 정상 원고의 문단 공백은 보존합니다. [재현과 해결](../find_debug/2026-11-10.md).

## 측정

```sh
.venv/Scripts/python benchmark_ollama.py --runs 3 --warmup
ollama ps
```

각 실행의 `first_token_s`, `total_s`, 글자 수가 JSON으로 출력됩니다. 짧은 테스트 프롬프트만 사용하며 사용자 원고는 출력하지 않습니다. 서버 로그에는 로딩 시간·입력 및 출력 토큰 수·추론 시간이 남습니다.

2026-11-10 로컬 모델에서 예열 후 짧은 3문장 생성 3회는 첫 토큰 **0.049~0.283초**, 전체 **0.80~2.35초**였습니다. 중앙값은 각각 0.056초, 0.939초입니다. 이 수치는 배포 서버나 장편 챕터의 성능 보장이 아니며, 변경 전과 동일 조건의 대조 실험도 아닙니다.

## 서버 조정 순서

1. `ollama ps`로 GPU/CPU 적재 상태를 확인합니다. CPU 또는 부분 GPU 적재는 지연의 원인이 될 수 있습니다.
2. 실제 Bible·Outline·이전 요약을 포함한 요청으로 첫 토큰·완료 시간과 메모리를 측정합니다. 서버 컨텍스트 길이는 입력과 출력 전체를 수용하도록 설정합니다. 속도를 위해 필수 컨텍스트를 잘라서는 안 됩니다.
3. `OLLAMA_MAX_CONCURRENT=1`에서 시작합니다. VRAM과 부하 시험을 보고 늘립니다. 값은 **FastAPI 프로세스별**이므로 worker 수를 늘리면 전체 동시성이 곱해집니다.
4. 요청이 슬롯을 5초 내 얻지 못하면 내부 JSON API는 503과 `Retry-After`를 반환합니다. 이미 시작한 챕터 SSE에서는 `error` 이벤트를 보냅니다. Spring Boot 외부 응답은 기존 502/스트림 실패 계약을 유지합니다. 자동 무한 재시도는 하지 않습니다.
5. 모델 상주는 메모리를 사용합니다. 다른 모델과 함께 운영할 때는 `OLLAMA_KEEP_ALIVE`와 Ollama 서버의 병렬 처리·대기열 설정을 함께 조정합니다.

공식 근거: [Chat API](https://docs.ollama.com/api/chat), [구조화 출력](https://docs.ollama.com/capabilities/structured-outputs), [모델 상주·GPU·동시 처리 FAQ](https://docs.ollama.com/faq).

## 실제 3장 파이프라인 추가 확인 (2026-11-10)

`.venv/Scripts/python validate_pipeline.py --chapters 3`은 합성 작품의 Bible·Outline·각 장의 생성·요약·Continuity를 실제 서비스 함수로 실행합니다. 임시 인물 저장소를 사용하며 제품의 검토·승인·DB 저장을 대신하지 않습니다.

첫 실행에서는 2장 생성이 출력 한도에 도달해 정상 결과 없이 실패했습니다. 당시 `/api/ps`는 컨텍스트 4096을 보고했습니다. 이후 요청·예열에 `OLLAMA_NUM_CTX=32768`을 명시한 실행은 3장 모두 완료했습니다. 다른 생성 결과를 사용하므로 출력 실패 원인이 컨텍스트 하나였다고 단정하거나 성능 개선율을 계산하지 않습니다.

| 단계 | 첫 토큰(초) | 요약·검증 포함 완료(초) | 본문 글자 수 | Continuity 점수 |
|---|---:|---:|---:|---:|
| 1장 | 0.254 | 23.570 | 3510 | 100 |
| 2장 | 0.250 | 22.602 | 3040 | 60 |
| 3장 | 0.267 | 26.843 | 3747 | 88 |

Bible은 21.842초, Outline은 15.364초였습니다. 2장에서는 Outline 인물의 전체 이름 미검출과 모델 지적이, 3장에서는 플롯 지적이 남았습니다. 일부 모델 지적 문장이 불완전하거나 평가와 설명이 맞지 않아 사람 검토가 필요합니다. 세 장 모두 요청한 4000~6000자보다 짧습니다. **생성 절차 완료이며 원고 품질 통과가 아닙니다.** 연속성 결과를 제거하거나 승인으로 바꾸지 않았습니다. 표본 한 작품으로 P95나 배포 성능을 주장하지 않습니다.

컨텍스트를 늘리면 메모리 요구량도 늘어납니다. 긴 입력에서 부족하면 값을 조정하고 GPU 적재 상태를 다시 확인합니다. 서버가 처리 가능한 입력 한도를 넘는 장편까지 보장하지 않습니다. [공식 컨텍스트 설명](https://docs.ollama.com/context-length), [num_ctx 파라미터](https://docs.ollama.com/modelfile).

## 슬롯 포화와 긴 입력 회복

`.venv/Scripts/python validate_load.py`는 슬롯 1개, 대기 제한 0.2초로 실제 스트림을 점유합니다. 경쟁 요청은 `LLMBusyError`로 약 0.215초에 거부됐고, 스트림을 닫은 뒤 이전 요약 40개·입력 7584자로 요청한 세 문장 출력은 첫 토큰 0.485초, 전체 1.266초, 115자로 완료됐습니다. 데이터는 합성이고 실제 40개 챕터 생성이나 지속 부하 검증은 아닙니다.
