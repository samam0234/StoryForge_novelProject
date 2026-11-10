-- 운영 조치는 단일·일괄 요청 모두 동일한 트랜잭션 잠금을 사용합니다.
CREATE TABLE moderation_guard (id integer PRIMARY KEY CHECK (id = 1));
INSERT INTO moderation_guard (id) VALUES (1);

-- 계정/신고 삭제 후에도 운영 이력을 보존하기 위해 FK를 두지 않습니다.
CREATE TABLE admin_audit (
    id bigserial PRIMARY KEY,
    actor_id bigint NOT NULL,
    actor_name varchar(255) NOT NULL,
    action varchar(30) NOT NULL,
    target_type varchar(30) NOT NULL,
    target_id bigint NOT NULL,
    report_id bigint,
    note varchar(300) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_audit_created ON admin_audit (created_at DESC, id DESC);
CREATE INDEX idx_admin_audit_actor ON admin_audit (actor_id, created_at DESC);
CREATE INDEX idx_report_queue_stable ON content_report (status, created_at DESC, id DESC);
CREATE INDEX idx_user_created ON app_user (created_at DESC, id DESC);
