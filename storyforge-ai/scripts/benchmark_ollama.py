"""짧은 동일 프롬프트를 반복해 첫 토큰과 전체 시간을 JSON으로 출력합니다. 원문은 기록하지 않습니다."""
import argparse
import asyncio
import json
import time

from app.core.config import get_settings
from app.core.ollama import OllamaLLM


async def benchmark(runs: int, warmup: bool) -> None:
    llm = OllamaLLM(get_settings())
    try:
        if warmup:
            await llm.warmup()
        for index in range(runs):
            start = time.perf_counter()
            first = None
            chars = 0
            async for chunk in llm.stream(system="한국어로 간결하게 답하세요.", prompt="비 오는 서점의 분위기를 세 문장으로 묘사하세요."):
                if first is None:
                    first = time.perf_counter() - start
                chars += len(chunk)
            print(json.dumps({"run": index + 1, "first_token_s": first, "total_s": time.perf_counter() - start,
                              "characters": chars, "warmup": warmup}))
    finally:
        await llm.aclose()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", type=int, default=3, choices=range(1, 11))
    parser.add_argument("--warmup", action="store_true")
    args = parser.parse_args()
    asyncio.run(benchmark(args.runs, args.warmup))
