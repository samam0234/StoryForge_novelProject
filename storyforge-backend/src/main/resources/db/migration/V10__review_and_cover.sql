-- 작업 기록의 앞뒤 본문은 공개 그래프에 넣지 않고, 참여자 조회에서만 비교합니다.
CREATE TABLE activity_review (
    event_id    BIGINT PRIMARY KEY REFERENCES activity_event (id) ON DELETE CASCADE,
    before_text TEXT,
    after_text  TEXT
);

ALTER TABLE project ADD COLUMN cover_url VARCHAR(500);
ALTER TABLE work ADD COLUMN cover_url VARCHAR(500);
