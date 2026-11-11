import pytest

from app.core.ollama import LLMError
from app.services.repetition_guard import RepetitionGuard


def test_long_sentence_loop_is_detected_across_chunks():
    guard = RepetitionGuard()
    sentence = "한서윤은 기록실에서 사라진 지도와 항해 일지를 나란히 펼쳐 두고 서로 다른 시각에 기록된 표식을 하나씩 대조했다. "
    with pytest.raises(LLMError, match="반복"):
        for character in sentence * 3:
            guard.feed(character)


def test_short_dialogue_and_two_recurring_sentences_are_allowed():
    guard = RepetitionGuard()
    guard.feed("네. " * 20)
    sentence = "같은 약속을 되새기면서도 이번에는 자신이 직접 선택한 길을 끝까지 걸어가겠다고 마음속으로 다짐했다. "
    guard.feed(sentence * 2)


def test_unpunctuated_loop_is_detected():
    guard = RepetitionGuard()
    with pytest.raises(LLMError):
        guard.feed("가나다라마" * 80)
