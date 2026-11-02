-- 프로젝트 설정(기획서 §9.6-5), 버전 관리(§6.1), 생성 히스토리(§6.4). docs/plan/should-features.md

ALTER TABLE project
    ADD COLUMN model         VARCHAR(60),
    ADD COLUMN default_tone  TEXT,
    ADD COLUMN export_format VARCHAR(10) NOT NULL DEFAULT 'md';

CREATE TABLE story_bible_version (
    id          BIGSERIAL PRIMARY KEY,
    bible_id    BIGINT  NOT NULL REFERENCES story_bible (id) ON DELETE CASCADE,
    version     INTEGER NOT NULL,
    snapshot    JSONB   NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (bible_id, version)
);

CREATE TABLE chapter_version (
    id          BIGSERIAL PRIMARY KEY,
    chapter_id  BIGINT      NOT NULL REFERENCES chapter (id) ON DELETE CASCADE,
    version     INTEGER     NOT NULL,
    source      VARCHAR(20) NOT NULL,
    title       VARCHAR(200),
    content     TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (chapter_id, version)
);

-- 기존 데이터의 현재 내용을 첫 이력으로 남깁니다.
INSERT INTO story_bible_version (bible_id, version, snapshot, created_at)
SELECT b.id, b.version,
       jsonb_build_object(
           'worldSetting', b.world_setting, 'theme', b.theme, 'tone', b.tone,
           'characters', COALESCE((SELECT jsonb_agg(jsonb_build_object(
                                       'name', c.name, 'description', c.description,
                                       'personality', c.personality, 'relationships', c.relationships) ORDER BY c.id)
                                   FROM "character" c WHERE c.bible_id = b.id), '[]'::jsonb)),
       b.updated_at
FROM story_bible b
WHERE b.version > 0;

INSERT INTO chapter_version (chapter_id, version, source, title, content, created_at)
SELECT id, 1, 'GENERATED', title, content, created_at
FROM chapter
WHERE content IS NOT NULL;
