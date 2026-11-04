-- 패키지 전체 Continuity 검사 결과. 작품 사이의 설정 충돌을 찾습니다. docs/plan/community.md

CREATE TABLE package_report (
    id              BIGSERIAL PRIMARY KEY,
    package_work_id BIGINT      NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    issues          JSONB       NOT NULL,
    score           INTEGER     NOT NULL,
    skipped_titles  JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_package_report ON package_report (package_work_id, created_at DESC);
