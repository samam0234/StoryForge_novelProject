"""본문 한 번의 입력으로 요약과 연속성 판단을 함께 수행합니다."""
from app.core.llm import LLMClient
from app.core.ollama import LLMError
from app.models.orchestration import chapter_prompt_context
from app.models.schemas import ChapterAnalysis, ChapterContext, ChapterReviewResult, ContinuityResult
from app.prompts.orchestration import render
from app.services.continuity_checker import rule_checks, score


async def analyze(llm: LLMClient, context: ChapterContext, content: str) -> ChapterReviewResult:
    result = await llm.structured(
        system=render("system"),
        prompt=render("chapter_analysis", **chapter_prompt_context(context), content=content),
        schema=ChapterAnalysis,
        model=context.project.model,
    )
    summary = result.summary.strip()
    if not summary:
        raise LLMError("챕터 요약이 비어 있어 생성 결과를 완료할 수 없습니다.")
    issues = rule_checks(context, content) + result.issues
    return ChapterReviewResult(summary=summary, report=ContinuityResult(issues=issues, score=score(issues)))
