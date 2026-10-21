-- 기획서 v1.2 §10 데이터 모델 초안 기준

CREATE TABLE project (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    genre       VARCHAR(100),
    status      VARCHAR(40)  NOT NULL,
    owner_id    BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE story_bible (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT  NOT NULL UNIQUE REFERENCES project (id) ON DELETE CASCADE,
    world_setting  TEXT,
    theme          TEXT,
    tone           TEXT,
    version        INTEGER NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE character (
    id             BIGSERIAL PRIMARY KEY,
    bible_id       BIGINT       NOT NULL REFERENCES story_bible (id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    personality    TEXT,
    relationships  TEXT,
    current_state  JSONB
);

CREATE TABLE outline (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL UNIQUE REFERENCES project (id) ON DELETE CASCADE,
    chapters    JSONB  NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE chapter (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT      NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    number              INTEGER     NOT NULL,
    title               VARCHAR(200),
    content             TEXT,
    status              VARCHAR(20) NOT NULL,
    summary             TEXT,
    generation_context  JSONB,
    created_at          TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, number)
);

CREATE TABLE continuity_report (
    id          BIGSERIAL PRIMARY KEY,
    chapter_id  BIGINT  NOT NULL REFERENCES chapter (id) ON DELETE CASCADE,
    issues      JSONB   NOT NULL,
    score       INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);
