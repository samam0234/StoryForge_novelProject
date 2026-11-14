"""장면 연결 로지스틱 회귀를 합성 문장으로 맞추고 가중치 JSON을 갱신합니다."""
from __future__ import annotations

import json
import math
from pathlib import Path

from app.services.plausibility import features

# 1은 끊긴 장면, 0은 바로 이어지는 장면입니다.
_ROWS = [
    ("문을 열고 안을 들여다보았다", "그리고 문 너머에는 꺼진 등 하나만 남아 있었다", 0),
    ("편지를 집어 수신인을 확인했다", "그 순간 수신인 칸에 자기 이름이 적혀 있었다", 0),
    ("열차가 떠나고 플랫폼에 섰다", "이어서 플랫폼에 남은 사람이 우산 하나를 내밀었다", 0),
    ("자물쇠를 열고 창고 안으로 들어갔다", "바로 그 창고 안에서 젖은 편지가 상자 위에 있었다", 0),
    ("회중시계의 멈춘 시각을 맞췄다", "그러나 시계 바늘은 다시 같은 시각에서 멈추었다", 0),
    ("쪽지의 주소를 소리 내어 읽었다", "그때 쪽지에 적힌 골목 입구가 창문 밖으로 보였다", 0),
    ("전표를 찾아 주머니에 넣었다", "그리고 전표의 잉크가 아직 손에 묻어 있었다", 0),
    ("빈 의자 위에 놓인 대사를 읽었다", "이윽고 그 대사 마지막 줄을 따라 입을 열었다", 0),
    ("밧줄을 감으며 배를 기다렸다", "문 너머가 아니라 부두 끝에서 같은 배가 다가왔다", 0),
    ("식은 커피를 두고 창밖을 보았다", "그 순간 창밖에 멈춘 차의 문이 열렸다", 0),
    ("악보를 꺼내 첫 소절을 짚었다", "그리고 그 소절을 소리 없이 한 번 더 따라갔다", 0),
    ("서랍에서 카드를 꺼냈다", "그러나 카드의 이름은 방금 전 서랍 목록과 달랐다", 0),
    ("문을 열고 안을 들여다보았다", "갑자기 몇 년 뒤 그는 사막의 우주선에서 눈을 떴다", 1),
    ("편지를 집어 수신인을 확인했다", "어느 날 왕궁의 무도회에서 전혀 다른 사람이 춤을 추었다", 1),
    ("열차가 떠나고 플랫폼에 섰다", "시간이 흘러 깊은 바닷속 연구소의 경보가 울렸다", 1),
    ("자물쇠를 열고 창고 안으로 들어갔다", "다음날 초등학교 운동회에서 아이들이 박수를 쳤다", 1),
    ("회중시계의 멈춘 시각을 맞췄다", "갑자기 중세 전장의 나팔 소리가 이야기를 새로 시작했다", 1),
    ("쪽지의 주소를 소리 내어 읽었다", "몇 년 후 달 기지의 사령실에서 다른 전쟁이 시작되었다", 1),
    ("전표를 찾아 주머니에 넣었다", "어느 날 아침 궁중 요리가 황제의 식탁에 올랐다", 1),
    ("빈 의자 위에 놓인 대사를 읽었다", "오랜만에 외계 함선의 선장이 성간 도약을 명령했다", 1),
    ("밧줄을 감으며 배를 기다렸다", "갑자기 눈보라 치는 산정에서 다른 시대의 이야기로 깨어났다", 1),
    ("식은 커피를 두고 창밖을 보았다", "며칠 뒤 전혀 다른 왕국의 대관식이 거행되었다", 1),
    ("악보를 꺼내 첫 소절을 짚었다", "시간이 흘러 해저 도시의 시장이 포고령을 낭독했다", 1),
    ("서랍에서 카드를 꺼냈다", "문득 천 년 전 신전의 제사장이 불을 붙였다", 1),
    ("손전등을 켜고 계단을 내려갔다", "그리고 손전등 불빛 끝에 같은 계단의 다음 칸이 보였다", 0),
    ("우산을 접고 골목으로 들어섰다", "그때 골목 끝에서 우산을 접은 사람이 뒤를 돌아보았다", 0),
    ("열쇠를 자물쇠에 넣고 돌렸다", "이어서 열쇠가 돌아간 문이 안쪽으로 열렸다", 0),
    ("젖은 편지를 난로 옆에 펼쳤다", "그러나 편지의 마지막 줄은 난로 불빛에서도 흐렸다", 0),
    ("손전등을 켜고 계단을 내려갔다", "며칠 뒤 왕실 연회에서 다른 왕자가 검을 뽑았다", 1),
    ("우산을 접고 골목으로 들어섰다", "갑자기 해저 도시의 사이렌이 울리며 새로운 전쟁이 시작되었다", 1),
    ("열쇠를 자물쇠에 넣고 돌렸다", "어느 날 우주 정거장의 선장이 출격을 명령했다", 1),
    ("젖은 편지를 난로 옆에 펼쳤다", "시간이 흘러 사막 대상의 행렬이 전혀 다른 도시를 떠났다", 1),
]


def _sigmoid(value: float) -> float:
    if value >= 0:
        return 1.0 / (1.0 + math.exp(-value))
    exp = math.exp(value)
    return exp / (1.0 + exp)


def main() -> None:
    raw = [features(prev, head) for prev, head, _ in _ROWS]
    labels = [float(label) for _, _, label in _ROWS]
    width = len(raw[0])
    mean = [sum(row[i] for row in raw) / len(raw) for i in range(width)]
    scale = []
    for i in range(width):
        var = sum((row[i] - mean[i]) ** 2 for row in raw) / len(raw)
        scale.append(math.sqrt(var) or 1.0)
    scaled = [[(row[i] - mean[i]) / scale[i] for i in range(width)] for row in raw]
    weights = [0.0] * width
    bias = 0.0
    for _ in range(1200):
        for row, label in zip(scaled, labels):
            error = _sigmoid(bias + sum(w * x for w, x in zip(weights, row))) - label
            for i in range(width):
                weights[i] -= 0.15 * error * row[i]
            bias -= 0.15 * error
    correct = 0
    for row, label in zip(scaled, labels):
        prob = _sigmoid(bias + sum(w * x for w, x in zip(weights, row)))
        correct += int((prob >= 0.5) == (label >= 0.5))
    payload = {
        "weights": [round(w, 6) for w in weights],
        "bias": round(bias, 6),
        "mean": [round(v, 6) for v in mean],
        "scale": [round(v, 6) for v in scale],
        "threshold": 0.62,
    }
    path = Path(__file__).resolve().parents[1] / "app" / "services" / "plausibility_weights.json"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"train accuracy={correct}/{len(labels)} wrote={path.name}")


if __name__ == "__main__":
    main()
