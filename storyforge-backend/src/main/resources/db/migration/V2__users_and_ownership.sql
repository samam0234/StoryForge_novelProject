-- JWT 인증과 프로젝트 소유자 (docs/plan/auth.md)

CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(254) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    display_name   VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
);

-- 인증 도입 전 프로젝트는 owner_id가 NULL로 남아 어떤 사용자에게도 보이지 않습니다.
ALTER TABLE project
    ADD CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE;

CREATE INDEX idx_project_owner ON project (owner_id);
