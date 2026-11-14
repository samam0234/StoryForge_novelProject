"""장면 연결 어색함을 점수로 내는 작은 로지스틱 회귀.

생성 모델을 받지 않습니다. 직전 장면과 이번 장 첫머리의 단어 겹침, 접속, 시간 도약으로
어색한 확률을 계산합니다. 검토를 통과시키거나 다른 이슈를 지우지 않습니다.
가중치는 scripts/train_plausibility.py가 합성 문장으로 맞춘 값입니다.
"""
from __future__ import annotations

import json
import math
import re
from functools import lru_cache
from pathlib import Path

_CONNECTIVES = ("그리고", "그러나", "그때", "직후", "이어서", "그 순간", "바로 그", "이윽고", "문 너머")
_SKIPS = ("다음날", "며칠", "몇 년", "몇년", "갑자기", "어느 날", "시간이 흘러", "오랜만에", "문득")
_WEIGHTS = Path(__file__).with_name("plausibility_weights.json")


def features(previous: str, opening: str) -> list[float]:
    """직전 장면과 새 첫머리에서 6개 연결 특징을 뽑습니다."""
    prev = previous.strip()
    head = opening.strip()[:180]
    prev_words = set(re.findall(r"[가-힣]{2,}", prev))
    head_words = set(re.findall(r"[가-힣]{2,}", head))
    union = prev_words | head_words
    shared = len(prev_words & head_words) / len(union) if union else 0.0
    tail_chars = set(prev[-24:])
    head_chars = set(head[:24])
    char_union = tail_chars | head_chars
    char_overlap = len(tail_chars & head_chars) / len(char_union) if char_union else 0.0
    connective = 1.0 if any(word in head[:16] for word in _CONNECTIVES) else 0.0
    skip = 1.0 if any(word in head[:40] for word in _SKIPS) else 0.0
    novel = len(head_words - prev_words) / len(head_words) if head_words else 1.0
    echo = 1.0 if len(prev) >= 8 and prev[-8:] in head else 0.0
    return [shared, char_overlap, connective, skip, novel, echo]


def _sigmoid(value: float) -> float:
    if value >= 0:
        return 1.0 / (1.0 + math.exp(-value))
    exp = math.exp(value)
    return exp / (1.0 + exp)


@lru_cache(maxsize=1)
def _model() -> dict:
    return json.loads(_WEIGHTS.read_text(encoding="utf-8"))


def awkward_probability(previous: str, opening: str) -> float:
    """1에 가까울수록 직전 장면과 끊긴 첫머리입니다. 직전 장면이 짧으면 0입니다."""
    if len(previous.strip()) < 12 or not opening.strip():
        return 0.0
    model = _model()
    raw = features(previous, opening)
    scaled = [(value - mean) / scale for value, mean, scale in zip(raw, model["mean"], model["scale"])]
    logit = model["bias"] + sum(weight * value for weight, value in zip(model["weights"], scaled))
    return _sigmoid(logit)


def is_awkward(previous: str, opening: str) -> bool:
    return awkward_probability(previous, opening) >= float(_model()["threshold"])
