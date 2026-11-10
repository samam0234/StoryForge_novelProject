-- 재검증 중 수정된 본문에 이전 요약·리포트를 덮어쓰지 않습니다.
ALTER TABLE chapter ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
