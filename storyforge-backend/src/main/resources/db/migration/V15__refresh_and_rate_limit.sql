-- 액세스 토큰 만료 뒤 다시 로그인하지 않도록 갱신 토큰을 둡니다. 원문은 저장하지 않습니다.
CREATE TABLE refresh_token (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash          CHAR(64)     NOT NULL UNIQUE,
    credential_version  BIGINT       NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

-- 서버가 여러 대여도 같은 1분 창을 공유합니다.
CREATE TABLE rate_limit_window (
    bucket        VARCHAR(200) PRIMARY KEY,
    window_start  BIGINT       NOT NULL,
    hits          INTEGER      NOT NULL
);
