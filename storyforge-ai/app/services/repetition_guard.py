"""반복 루프가 지속되면 스트림을 중단해 추론 시간과 중복 원고 저장을 줄입니다."""
import re
from collections import Counter

from app.core.ollama import LLMError


class RepetitionGuard:
    def __init__(self) -> None:
        self.pending = ""
        self.sentences: Counter[str] = Counter()

    def feed(self, chunk: str) -> None:
        self.pending += chunk
        pieces = re.split(r"(?<=[.!?。])\s+|\n+", self.pending)
        self.pending = pieces.pop()
        for sentence in pieces:
            normalized = re.sub(r"\s+", "", sentence)
            if len(normalized) < 40:
                continue
            self.sentences[normalized] += 1
            if self.sentences[normalized] >= 3:
                raise LLMError("같은 긴 문장이 반복되어 생성을 중단했습니다. 장면 목표를 구체화한 뒤 다시 생성하세요.")
        # 문장 구분 없이 반복하는 출력도 제한합니다.
        tail = re.sub(r"\s+", "", self.pending[-2000:])
        for width in (80, 120, 160, 240):
            if len(tail) >= width * 3 and tail[-width:] * 3 == tail[-width * 3:]:
                raise LLMError("본문이 반복 루프에 빠져 생성을 중단했습니다. 다시 생성하세요.")
