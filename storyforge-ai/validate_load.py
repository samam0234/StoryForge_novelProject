"""합성 장기 컨텍스트, 실제 Ollama 슬롯 포화·중단 후 복구 검증. 원문은 출력하지 않습니다."""
import asyncio
import json
import time

from app.core.config import Settings
from app.core.ollama import LLMBusyError, OllamaLLM


async def main():
    settings = Settings(ollama_max_concurrent=1, ollama_queue_timeout=0.2)
    llm = OllamaLLM(settings)
    try:
        await llm.warmup()
        # 실제 스트림을 열린 채 유지해, 외부 모델 응답 중 경쟁 요청이 거부되는지 확인합니다.
        stream = llm.stream(system="한국어로 소설을 작성하세요.", prompt="비 오는 등대의 사건을 2000자로 쓰세요.")
        try:
            await asyncio.wait_for(stream.__anext__(), 60)
            started = time.perf_counter()
            try:
                await llm.text(system="간결하게 답하세요.", prompt="안녕", max_tokens=32)
            except LLMBusyError:
                print(json.dumps({"stage": "busy_rejected", "elapsed_s": time.perf_counter() - started}), flush=True)
            else:
                raise AssertionError("busy request unexpectedly succeeded")
        finally:
            await stream.aclose()
        summaries = "\n".join(
            f"{i}장: 기록관 한서윤은 제{i} 항로의 일지와 기상 관측표를 대조했다. "
            "등대의 전원은 수동 발전기에서 공급된다. 조수 민우는 관측 기록을 시간 순서로 정리하고 "
            "서로 다른 증언을 지도에 표시했다. 현장에는 두 사람만 남았고 배는 다음 날 아침에 온다. "
            "낡은 창고의 열쇠는 서윤이 보관한다. 일지는 훼손하지 않고 발견한 순서를 유지한다."
            for i in range(1, 41))
        prompt = ("Story Bible: 초자연적 능력이 없는 해안 마을. 서윤은 기록관, 민우는 조수.\n"
                  + summaries + "\nOutline: 41장, 날이 밝자 도착한 배에 기록을 전달한다. "
                  "이전 사건을 반복하지 말고 다음 장의 시작만 세 문장으로 쓰세요.")
        started = time.perf_counter()
        first = None
        characters = 0
        async def collect():
            nonlocal first, characters
            async for chunk in llm.stream(system="주어진 세계관과 이전 기록을 유지하세요.", prompt=prompt):
                if first is None:
                    first = time.perf_counter() - started
                characters += len(chunk)
        await asyncio.wait_for(collect(), 180)
        assert characters > 0
        print(json.dumps({"stage": "long_context_after_cancel", "input_characters": len(prompt),
                          "previous_chapters": 40, "first_token_s": first,
                          "total_s": time.perf_counter() - started, "output_characters": characters}), flush=True)
    finally:
        await llm.aclose()


if __name__ == "__main__":
    asyncio.run(main())
