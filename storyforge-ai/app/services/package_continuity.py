"""패키지 전체 Continuity 검사: 묶인 작품끼리 인물, 세계 규칙, 연표가 어긋나는지 봅니다.

규칙 기반 검사는 항상 실행하고, 실제 LLM이 연결되어 있으면 LLM 검증 결과를 더합니다.
같은 이름이 같은 인물이라는 보장은 없으므로 규칙 검사는 "확인이 필요하다"까지만 알립니다.
"""
from app.core.llm import LLMClient
from app.models.schemas import PackageContinuityRequest, PackageContinuityResult, PackageIssue, PackageIssues
from app.prompts.orchestration import render

_PENALTY = {"high": 30, "medium": 12, "low": 4}


def rule_checks(request: PackageContinuityRequest) -> list[PackageIssue]:
    issues: list[PackageIssue] = []
    seen: dict[str, list[tuple[str, str]]] = {}
    for work in request.works:
        for c in work.bible.characters:
            if c.name and c.description:
                seen.setdefault(c.name, []).append((work.title, c.description))
    for name, entries in seen.items():
        if len({text.strip() for _, text in entries}) > 1:
            issues.append(
                PackageIssue(
                    type="character_consistency",
                    description=f"'{name}'의 설명이 작품마다 다릅니다. 같은 인물이라면 설정을 맞추세요.",
                    severity="medium",
                    work_titles=[title for title, _ in entries],
                )
            )
    if request.links:
        linked = {t for link in request.links for t in (link.from_title, link.to_title)}
        for work in request.works:
            if work.title not in linked:
                issues.append(
                    PackageIssue(
                        type="plot_thread",
                        description=f"「{work.title}」은(는) 다른 작품과 연결되어 있지 않습니다.",
                        severity="low",
                        work_titles=[work.title],
                    )
                )
    return issues


def score(issues: list[PackageIssue]) -> int:
    return max(0, 100 - sum(_PENALTY[i.severity] for i in issues))


async def check_package(llm: LLMClient, request: PackageContinuityRequest) -> PackageContinuityResult:
    issues = rule_checks(request)
    if not llm.is_mock:
        found = await llm.structured(
            system=render("system"),
            prompt=render("package_continuity", request=request),
            schema=PackageIssues,
        )
        issues.extend(found.issues)
    return PackageContinuityResult(issues=issues, score=score(issues))
