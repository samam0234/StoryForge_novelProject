-- 승인 전 재요약·재검증 대상 표시. 생성 뒤 고쳤거나 Clone으로 가져온 원고입니다.

ALTER TABLE chapter ADD COLUMN needs_review BOOLEAN NOT NULL DEFAULT FALSE;

-- 검토 중인 챕터의 마지막 이력이 수정·Clone이면 요약이 본문과 어긋날 수 있습니다.
UPDATE chapter c
SET needs_review = TRUE
WHERE c.status = 'DRAFT'
  AND (SELECT v.source FROM chapter_version v WHERE v.chapter_id = c.id ORDER BY v.version DESC LIMIT 1)
      IN ('EDITED', 'CLONED');
