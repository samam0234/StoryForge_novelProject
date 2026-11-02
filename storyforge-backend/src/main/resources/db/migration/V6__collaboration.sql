-- 함께 창작: 초대된 사람만 원고와 작업 커밋을 봅니다. 파이프라인 상태는 바꾸지 않습니다.

CREATE TABLE project_collaborator (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    invited_by BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    status     VARCHAR(12) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, user_id)
);
CREATE INDEX idx_collab_user ON project_collaborator (user_id, status);

-- 작업 커밋. 메시지와 인용 구간은 프로젝트 참여자에게만 돌려줍니다.
-- 공개 작업 그래프에는 본문 없는 "작업 커밋" 요약만 이어집니다.
CREATE TABLE work_commit (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT       NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    author_id      BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    parent_id      BIGINT       REFERENCES work_commit (id) ON DELETE SET NULL,
    message        VARCHAR(500) NOT NULL,
    target         VARCHAR(12)  NOT NULL,
    chapter_number INTEGER,
    start_offset   INTEGER,
    end_offset     INTEGER,
    quote          TEXT,
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_commit_project ON work_commit (project_id, created_at DESC);
