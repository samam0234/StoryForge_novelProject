-- 신고·관리자 조치, 사용자 차단, 알림, 계정 정지, 이용약관 동의. docs/plan/moderation.md

ALTER TABLE app_user
    ADD COLUMN suspended_at      TIMESTAMPTZ,
    ADD COLUMN suspended_reason  VARCHAR(300),
    ADD COLUMN terms_accepted_at TIMESTAMPTZ,
    ADD COLUMN terms_version     VARCHAR(20);

-- 숨김은 삭제가 아닙니다. 관리자가 복원할 수 있습니다.
ALTER TABLE work ADD COLUMN hidden_at TIMESTAMPTZ;
ALTER TABLE post ADD COLUMN hidden_at TIMESTAMPTZ;
ALTER TABLE comment ADD COLUMN hidden_at TIMESTAMPTZ;

CREATE TABLE content_report (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    target_type     VARCHAR(10)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    reason          VARCHAR(20)  NOT NULL,
    detail          VARCHAR(500),
    status          VARCHAR(10)  NOT NULL,
    resolved_by     BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    resolution_note VARCHAR(300),
    created_at      TIMESTAMPTZ  NOT NULL,
    resolved_at     TIMESTAMPTZ
);
CREATE INDEX idx_report_queue ON content_report (status, created_at DESC);
CREATE INDEX idx_report_target ON content_report (target_type, target_id, status);
-- 처리 전 중복 신고 방지
CREATE UNIQUE INDEX uq_report_open ON content_report (reporter_id, target_type, target_id) WHERE status = 'OPEN';

CREATE TABLE user_block (
    id         BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE notification (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type       VARCHAR(20)  NOT NULL,
    message    VARCHAR(300) NOT NULL,
    link       VARCHAR(300),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notification_user ON notification (user_id, created_at DESC);
