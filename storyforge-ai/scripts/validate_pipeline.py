"""격리된 합성 작품으로 실제 Ollama 생성·검증 경로를 측정합니다.

제품 승인이나 DB 저장을 수행하지 않습니다. 원문 대신 시간·길이·이슈만 출력합니다.
"""
import argparse
import asyncio
from contextlib import aclosing
import json
import logging
import shutil
import tempfile
import time
from pathlib import Path
from unittest.mock import patch

from app.core.config import Settings
from app.core.ollama import OllamaLLM
from app.models.schemas import (
    BibleGenerateRequest, BibleInfo, ChapterContext, ChapterGenerateResult, OutlineGenerateRequest,
    PreviousChapter, ProjectInfo,
)
from app.models.orchestration import Done
from app.services.orchestration import AiTaskOrchestrator
from app.services.character_bank import CharacterBank


def emit(stage, **values):
    print(json.dumps({"stage": stage, **values}, ensure_ascii=True), flush=True)


async def validate(chapters: int, timeout: float):
    settings = Settings(llm_provider="ollama")
    llm = OllamaLLM(settings)
    tasks = AiTaskOrchestrator(llm)
    project = ProjectInfo(title="검증용 등대의 기록", genre="미스터리", model=settings.ollama_model)
    stage = "warmup"
    try:
        await asyncio.wait_for(llm.warmup(), timeout)
        with tempfile.TemporaryDirectory(prefix="storyforge-validation-") as directory:
            root = Path(directory)
            shutil.copyfile(Path(__file__).resolve().parents[1] / "data/names.json", root / "names.json")
            bank = CharacterBank(root)
            stage = "bible"
            started = time.perf_counter()
            with patch("app.services.character_bank.CharacterBank", return_value=bank):
                result = await asyncio.wait_for(tasks.generate_bible(BibleGenerateRequest(
                    project=project, prompt="폭풍으로 고립된 등대에서 네 사람이 사라진 항해 일지를 찾는다. "
                    "초자연적 능력은 없고 이동은 도보와 배로만 가능하다. 하루 동안 진행되는 완결된 이야기."
                )), timeout)
            bible = BibleInfo.model_validate(result.model_dump())
            emit(stage, total_s=time.perf_counter() - started, characters=len(bible.characters))
            stage = "outline"
            started = time.perf_counter()
            outline = await asyncio.wait_for(tasks.generate_outline(OutlineGenerateRequest(
                project=project, bible=bible, chapter_count=chapters)), timeout)
            if len(outline.chapters) != chapters:
                raise ValueError("requested chapter count mismatch")
            emit(stage, total_s=time.perf_counter() - started, chapters=len(outline.chapters))
            previous = []
            for chapter in outline.chapters:
                stage = f"chapter_{chapter.order}"
                ctx = ChapterContext(project=project, bible=bible, chapter_number=chapter.order,
                                     outline_chapter=chapter, previous_chapters=list(previous),
                                     story_outline=outline.chapters)
                started = time.perf_counter()
                async def collect() -> tuple[ChapterGenerateResult, float]:
                    first = None
                    final = None
                    async with aclosing(tasks.generate_chapter(ctx)) as generation:
                        async for event in generation:
                            if isinstance(event, Done):
                                final = event.result
                            elif first is None:
                                first = time.perf_counter() - started
                    if final is None or first is None or not final.content.strip() or not final.summary.strip():
                        raise ValueError("missing completed chapter, summary or first token")
                    return final, first

                final, first = await asyncio.wait_for(collect(), timeout)
                emit(stage, first_token_s=first, total_s=time.perf_counter() - started,
                     characters=len(final.content), summary_characters=len(final.summary),
                     previous_chapters=len(previous), score=final.report.score,
                     issues=[i.model_dump() for i in final.report.issues])
                # 합성 시험의 다음 입력만 조립합니다. 제품 CHAPTER_REVIEW 승인이 아닙니다.
                for earlier in previous:
                    earlier.ending_excerpt = None
                previous.append(PreviousChapter(number=chapter.order, title=final.title, summary=final.summary,
                                                ending_excerpt=final.content[-1200:]))
            emit("complete", chapters=len(previous), model=settings.ollama_model)
    except Exception as exc:
        emit("failed", operation=stage, error_type=type(exc).__name__)
        raise
    finally:
        await llm.aclose()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--chapters", type=int, choices=range(2, 6), default=3)
    parser.add_argument("--timeout", type=float, default=180)
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    asyncio.run(validate(args.chapters, args.timeout))
