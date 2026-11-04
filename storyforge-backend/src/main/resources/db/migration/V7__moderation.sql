-- 공개 운영 전 관리 수단 (SECURITY.md, docs/plan/community.md)
-- 이용약관 동의, 관리자, 신고, 차단, 숨김

ALTER TABLE app_user
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER',
    ADD COLUMN terms_accepted_at TIMESTAMPTZ;

ALTER TABLE post ADD COLUMN hidden_at TIMESTAMPTZ;
ALTER TABLE work ADD COLUMN hidden_at TIMESTAMPTZ;
ALTER TABLE comment ADD COLUMN hidden_at TIMESTAMPTZ;

CREATE TABLE content_report (
    id           BIGSERIAL PRIMARY KEY,
    reporter_id  BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    target_type  VARCHAR(16)  NOT NULL,
    target_id    BIGINT       NOT NULL,
    reason       VARCHAR(32)  NOT NULL,
    detail       VARCHAR(1000),
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    resolved_at  TIMESTAMPTZ,
    resolver_id  BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_content_report_open
    ON content_report (reporter_id, target_type, target_id)
    WHERE status = 'OPEN';
CREATE INDEX idx_content_report_status ON content_report (status, created_at DESC);

CREATE TABLE user_block (
    blocker_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
CREATE INDEX idx_user_block_blocked ON user_block (blocked_id);
