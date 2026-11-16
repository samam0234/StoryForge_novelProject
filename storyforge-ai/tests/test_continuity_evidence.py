from app.models.schemas import ContinuityIssue
from app.services.continuity_checker import verified_evidence


def test_legacy_issue_remains_readable():
    issue = ContinuityIssue(type="timeline", description="시간 확인", severity="low")
    assert issue.evidence is None
    assert issue.reference is None
    assert issue.suggestion is None


def test_only_literal_manuscript_evidence_is_retained():
    actual = ContinuityIssue(type="timeline", description="시간 확인", severity="low",
                            evidence="그는 아침에 떠났다.", reference="1장 요약", suggestion="시간을 확인하세요.")
    invented = actual.model_copy(update={"evidence": "그는 저녁에 떠났다."})
    result = verified_evidence([actual, invented], "그는 아침에 떠났다. 비가 내렸다.")
    assert result[0].evidence == actual.evidence
    assert result[1].evidence is None
    assert result[1].reference == "1장 요약"
    assert invented.evidence == "그는 저녁에 떠났다."
