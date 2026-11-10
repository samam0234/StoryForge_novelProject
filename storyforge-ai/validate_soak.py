"""실제 Ollama의 반복 챕터 생성·포화 거부·중단 후 복구를 측정합니다.

합성 입력만 사용하며 DB 저장이나 사용자 승인 없이 서비스 함수를 호출합니다.
생성 원문은 출력하지 않습니다. 성능 수치는 실행 PC와 설정에 한정됩니다.
"""
import argparse
import asyncio
from contextlib import aclosing
import json
import math
import time

from app.core.config import Settings
from app.core.ollama import LLMBusyError, OllamaLLM
from app.models.schemas import ChapterContext, ChapterGenerateResult
from app.services.chapter_generator import Done, generate_chapter


def emit(stage, **values):
    # Windows cp949 콘솔에서도 모델이 반환한 모든 유니코드를 손실 없이 기록합니다.
    print(json.dumps({"stage": stage, **values}, ensure_ascii=True), flush=True)


def context(model):
    return ChapterContext.model_validate({
        "project": {"title": "검증용 조수 기록", "genre": "미스터리", "model": model},
        "bible": {
            "worldSetting": "초자연적 능력이 없는 해안 마을. 폭풍으로 배가 끊긴 등대. 이동은 도보와 배로만 한다.",
            "theme": "기록을 함께 확인하며 신뢰를 회복한다", "tone": "차분한 한국어 문학 소설",
            "characters": [
                {"name": "한서윤", "description": "항해 기록관", "personality": "증거를 확인하고 신중히 판단한다",
                 "relationships": "박민우의 동료", "currentState": {"location": "등대 기록실", "key": "창고 열쇠 보관"}},
                {"name": "박민우", "description": "등대 조수", "personality": "관측 시간을 정확하게 기록한다",
                 "relationships": "한서윤의 동료", "currentState": {"location": "등대 기록실"}},
            ],
        },
        "chapterNumber": 2,
        "previousChapters": [{"number": 1, "title": "잠긴 창고",
            "summary": "한서윤과 박민우는 등대 기록실에서 사라진 항해 일지의 빈자리를 발견했다. "
                       "폭풍으로 배가 끊겼고 두 사람만 등대에 남았다. 창고 열쇠는 한서윤이 보관한다. "
                       "박민우는 관측표에 적힌 오후 세 시의 시간을 다시 대조하기로 했다."}],
        "outlineChapter": {
            "order": 2, "title": "시간의 차이", "characters": ["한서윤", "박민우"],
            "beats": ["기록실에서 오후 세 시의 관측표와 벽시계의 시각을 대조한다",
                      "두 사람이 함께 창고를 열고 젖은 지도 뒤에서 항해 일지를 찾는다",
                      "원본을 훼손하지 않고 기록의 순서를 정리한다",
                      "날이 밝아 배가 도착할 때까지 확인한 사실을 함께 기록하기로 한다"],
            "goals": "성급한 의심을 거두고 확인 가능한 증거를 공유한다",
            "continuityPoints": ["열쇠는 한서윤이 보관", "초자연 현상과 다른 인물 없음", "배는 다음 날 아침 도착"],
        },
    })


def percentile(values, fraction):
    if not values:
        return None
    return round(sorted(values)[max(0, math.ceil(len(values) * fraction) - 1)], 3)


async def validate(duration, timeout):
    settings = Settings(llm_provider="ollama", ollama_max_concurrent=1, ollama_queue_timeout=0.2)
    llm = OllamaLLM(settings)
    ctx = context(settings.ollama_model)
    started = time.perf_counter()
    totals, first_tokens, lengths = [], [], []
    failures = busy = cancellations = rounds = consecutive_failures = 0
    try:
        await asyncio.wait_for(llm.warmup(), timeout)
        emit("started", duration_s=duration, model=settings.ollama_model,
             context_tokens=settings.ollama_num_ctx, max_concurrent=1, queue_timeout_s=0.2,
             scope="service_functions_only")
        # 짧은 시험도 예열만으로 끝나지 않고 실제 생성·검증을 한 번은 수행합니다.
        while rounds == 0 or time.perf_counter() - started < duration:
            rounds += 1
            try:
                # 가장 바깥 생성기를 닫은 직후 같은 클라이언트를 재사용합니다.
                if rounds % 3 == 1:
                    async with aclosing(generate_chapter(llm, ctx)) as cancelled:
                        await asyncio.wait_for(cancelled.__anext__(), timeout)
                    cancellations += 1
                began = time.perf_counter()
                async def collect() -> tuple[ChapterGenerateResult, float]:
                    nonlocal busy
                    first = None
                    final = None
                    async with aclosing(generate_chapter(llm, ctx)) as generation:
                        async for event in generation:
                            if isinstance(event, Done):
                                final = event.result
                            elif first is None:
                                first = time.perf_counter() - began
                                # 스트림이 슬롯을 점유한 동안 다른 요청은 제한 시간에 거부되어야 합니다.
                                try:
                                    await llm.text(system="검증용", prompt="한 단어로 답하세요", max_tokens=32)
                                except LLMBusyError:
                                    busy += 1
                                else:
                                    raise AssertionError("포화 요청이 거부되지 않았습니다")
                    if final is None or first is None or not final.content.strip() or not final.summary.strip():
                        raise AssertionError("본문·요약·첫 조각·검증 완료가 누락됐습니다")
                    return final, first

                final, first = await asyncio.wait_for(collect(), timeout)
                elapsed = time.perf_counter() - began
                totals.append(elapsed)
                first_tokens.append(first)
                lengths.append(len(final.content))
                consecutive_failures = 0
                emit("round", number=rounds, elapsed_s=round(elapsed, 3),
                     first_token_s=round(first, 3), characters=len(final.content),
                     score=final.report.score, issues=[i.model_dump() for i in final.report.issues],
                     elapsed_total_s=round(time.perf_counter() - started, 3))
            except Exception as exc:
                failures += 1
                consecutive_failures += 1
                emit("round_failed", number=rounds, error_type=type(exc).__name__,
                     elapsed_total_s=round(time.perf_counter() - started, 3))
                if consecutive_failures >= 3:
                    emit("stopped", reason="three_consecutive_failures")
                    break
    finally:
        await llm.aclose()
        emit("summary", elapsed_s=round(time.perf_counter() - started, 3), rounds=rounds,
             completed=len(totals), failures=failures, busy_rejected=busy,
             cancelled=cancellations, first_token_p50_s=percentile(first_tokens, 0.5),
             first_token_p95_s=percentile(first_tokens, 0.95), total_p50_s=percentile(totals, 0.5),
             total_p95_s=percentile(totals, 0.95), min_characters=min(lengths, default=None),
             max_characters=max(lengths, default=None), target_length_met=sum(4000 <= n <= 6000 for n in lengths))
    return 1 if failures else 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--duration", type=float, default=60, help="생성 시험의 최소 실행 시간(초)")
    parser.add_argument("--timeout", type=float, default=180, help="한 회 생성·요약·검증의 제한 시간(초)")
    args = parser.parse_args()
    if not math.isfinite(args.duration) or not 1 <= args.duration <= 7200:
        parser.error("--duration must be finite and between 1 and 7200")
    if not math.isfinite(args.timeout) or not 1 <= args.timeout <= 600:
        parser.error("--timeout must be finite and between 1 and 600")
    raise SystemExit(asyncio.run(validate(args.duration, args.timeout)))
