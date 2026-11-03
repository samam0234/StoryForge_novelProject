-- 동시 요청 방지용 낙관적 락 (docs/plan/stability.md)
ALTER TABLE project ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
