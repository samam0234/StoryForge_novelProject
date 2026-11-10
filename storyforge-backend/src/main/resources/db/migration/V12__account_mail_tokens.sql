ALTER TABLE app_user ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE account_mail_token (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    purpose VARCHAR(16) NOT NULL CHECK (purpose IN ('verify', 'reset')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    credential_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, purpose)
);
