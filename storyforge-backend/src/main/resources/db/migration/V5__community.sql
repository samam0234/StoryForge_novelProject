-- 커뮤니티: 게시 작품, Clone, 공유, 패키지포지, 게시글, 활동·업적, 프로필. docs/plan/community.md

ALTER TABLE app_user
    ADD COLUMN bio          VARCHAR(500),
    ADD COLUMN website      VARCHAR(300),
    ADD COLUMN avatar_color VARCHAR(7);

CREATE TABLE work (
    id               BIGSERIAL PRIMARY KEY,
    author_id        BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- 원본 프로젝트를 지워도 게시본은 남습니다.
    project_id       BIGINT       UNIQUE REFERENCES project (id) ON DELETE SET NULL,
    -- Clone한 프로젝트를 게시하면 원본 작품을 가리킵니다 (Clone 네트워크).
    parent_work_id   BIGINT       REFERENCES work (id) ON DELETE SET NULL,
    parent_title     VARCHAR(200),
    kind             VARCHAR(10)  NOT NULL,
    package_type     VARCHAR(20),
    title            VARCHAR(200) NOT NULL,
    synopsis         TEXT,
    genre            VARCHAR(100),
    tags             VARCHAR(300),
    visibility       VARCHAR(10)  NOT NULL,
    clone_policy     VARCHAR(10)  NOT NULL,
    downloadable     BOOLEAN      NOT NULL,
    world_note       TEXT,
    bible_snapshot   JSONB,
    outline_snapshot JSONB,
    revision         INTEGER      NOT NULL,
    -- 패키지는 묶인 작품 수를 chapter_count에 둡니다.
    chapter_count    INTEGER      NOT NULL DEFAULT 0,
    char_count       INTEGER      NOT NULL DEFAULT 0,
    like_count       INTEGER      NOT NULL DEFAULT 0,
    comment_count    INTEGER      NOT NULL DEFAULT 0,
    clone_count      INTEGER      NOT NULL DEFAULT 0,
    share_count      INTEGER      NOT NULL DEFAULT 0,
    view_count       BIGINT       NOT NULL DEFAULT 0,
    published_at     TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_work_board ON work (kind, visibility, published_at DESC);
CREATE INDEX idx_work_author ON work (author_id);
CREATE INDEX idx_work_parent ON work (parent_work_id);

CREATE TABLE work_chapter (
    id       BIGSERIAL PRIMARY KEY,
    work_id  BIGINT  NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    number   INTEGER NOT NULL,
    title    VARCHAR(200),
    content  TEXT,
    summary  TEXT,
    UNIQUE (work_id, number)
);

-- 게시본 변경 이력. 수정할 때마다 revision이 하나씩 오릅니다.
CREATE TABLE work_revision (
    id            BIGSERIAL PRIMARY KEY,
    work_id       BIGINT       NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    revision      INTEGER      NOT NULL,
    message       VARCHAR(300),
    chapter_count INTEGER      NOT NULL,
    char_count    INTEGER      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    UNIQUE (work_id, revision)
);

CREATE TABLE work_like (
    work_id    BIGINT      NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (work_id, user_id)
);
CREATE INDEX idx_work_like_recent ON work_like (work_id, created_at);

CREATE TABLE work_clone (
    id              BIGSERIAL PRIMARY KEY,
    work_id         BIGINT      NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    project_id      BIGINT      REFERENCES project (id) ON DELETE SET NULL,
    mode            VARCHAR(10) NOT NULL,
    revision        INTEGER     NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_work_clone_work ON work_clone (work_id, created_at DESC);

ALTER TABLE project
    ADD COLUMN form                 VARCHAR(10) NOT NULL DEFAULT 'LONG',
    ADD COLUMN cloned_from_work_id  BIGINT REFERENCES work (id) ON DELETE SET NULL,
    ADD COLUMN cloned_from_title    VARCHAR(200),
    ADD COLUMN cloned_from_revision INTEGER,
    ADD COLUMN clone_mode           VARCHAR(10);

CREATE TABLE work_share (
    id          BIGSERIAL PRIMARY KEY,
    work_id     BIGINT       NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    channel     VARCHAR(20)  NOT NULL,
    target      VARCHAR(200),
    token       VARCHAR(32)  NOT NULL UNIQUE,
    visit_count INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_work_share_work ON work_share (work_id, created_at DESC);

-- 패키지포지: 패키지(work.kind = PACKAGE)에 묶인 단독 작품
CREATE TABLE package_item (
    id              BIGSERIAL PRIMARY KEY,
    package_work_id BIGINT       NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    member_work_id  BIGINT       NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    position        INTEGER      NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    note            VARCHAR(500),
    -- 다른 작가의 작품은 그 작가가 승인해야 APPROVED가 됩니다.
    status          VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    UNIQUE (package_work_id, member_work_id)
);
CREATE INDEX idx_package_item_member ON package_item (member_work_id, status);

-- 패키지 안 작품 사이의 연결 (세계관 지도)
CREATE TABLE package_link (
    id              BIGSERIAL PRIMARY KEY,
    package_work_id BIGINT       NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    from_item_id    BIGINT       NOT NULL REFERENCES package_item (id) ON DELETE CASCADE,
    to_item_id      BIGINT       NOT NULL REFERENCES package_item (id) ON DELETE CASCADE,
    link_type       VARCHAR(20)  NOT NULL,
    label           VARCHAR(200),
    created_at      TIMESTAMPTZ  NOT NULL,
    CHECK (from_item_id <> to_item_id)
);

-- 창작 라운지 일반 게시글
CREATE TABLE post (
    id            BIGSERIAL PRIMARY KEY,
    author_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    category      VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    like_count    INTEGER      NOT NULL DEFAULT 0,
    comment_count INTEGER      NOT NULL DEFAULT 0,
    view_count    BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_post_list ON post (category, created_at DESC);

CREATE TABLE post_like (
    post_id    BIGINT      NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (post_id, user_id)
);

-- 작품·게시글 댓글. 대상은 둘 중 하나입니다.
CREATE TABLE comment (
    id         BIGSERIAL PRIMARY KEY,
    work_id    BIGINT REFERENCES work (id) ON DELETE CASCADE,
    post_id    BIGINT REFERENCES post (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    parent_id  BIGINT REFERENCES comment (id) ON DELETE CASCADE,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK ((work_id IS NULL) <> (post_id IS NULL))
);
CREATE INDEX idx_comment_work ON comment (work_id, created_at);
CREATE INDEX idx_comment_post ON comment (post_id, created_at);

-- 작업 그래프와 활동 잔디의 원본. actor로 수동(MANUAL)과 에이전트(AGENT)를 나눕니다.
-- 요약에는 본문을 넣지 않습니다 (게시 후 공개되므로).
CREATE TABLE activity_event (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    project_id    BIGINT      REFERENCES project (id) ON DELETE SET NULL,
    work_id       BIGINT      REFERENCES work (id) ON DELETE SET NULL,
    post_id       BIGINT      REFERENCES post (id) ON DELETE SET NULL,
    type          VARCHAR(30) NOT NULL,
    actor         VARCHAR(10) NOT NULL,
    summary       VARCHAR(300) NOT NULL,
    changed_chars INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_activity_user ON activity_event (user_id, created_at DESC);
CREATE INDEX idx_activity_project ON activity_event (project_id, created_at DESC);
CREATE INDEX idx_activity_work ON activity_event (work_id, created_at DESC);

CREATE TABLE user_achievement (
    user_id   BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    code      VARCHAR(40) NOT NULL,
    earned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, code)
);

-- 이슈 게시판의 달성 축하. 같은 작품·지표·단위는 한 번만 기록합니다.
CREATE TABLE issue_event (
    id         BIGSERIAL PRIMARY KEY,
    work_id    BIGINT      NOT NULL REFERENCES work (id) ON DELETE CASCADE,
    metric     VARCHAR(20) NOT NULL,
    threshold  INTEGER     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (work_id, metric, threshold)
);
CREATE INDEX idx_issue_recent ON issue_event (created_at DESC);

-- 기존 챕터 이력을 작업 그래프에 옮깁니다. 글자 수는 생성본 전체, 수정본은 직전 이력과 길이 차이로 근사합니다.
INSERT INTO activity_event (user_id, project_id, type, actor, summary, changed_chars, created_at)
SELECT p.owner_id, p.id,
       CASE v.source WHEN 'GENERATED' THEN 'CHAPTER_GENERATED' ELSE 'CHAPTER_EDITED' END,
       CASE v.source WHEN 'GENERATED' THEN 'AGENT' ELSE 'MANUAL' END,
       c.number || CASE v.source WHEN 'GENERATED' THEN '장 AI 생성' ELSE '장 직접 수정' END,
       CASE v.source
           WHEN 'GENERATED' THEN length(COALESCE(v.content, ''))
           ELSE abs(length(COALESCE(v.content, '')) - length(COALESCE(prev.content, '')))
       END,
       v.created_at
FROM chapter_version v
         JOIN chapter c ON c.id = v.chapter_id
         JOIN project p ON p.id = c.project_id
         LEFT JOIN chapter_version prev ON prev.chapter_id = v.chapter_id AND prev.version = v.version - 1
WHERE p.owner_id IS NOT NULL;
