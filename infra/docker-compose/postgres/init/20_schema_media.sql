-- Schema: media
CREATE TABLE IF NOT EXISTS t_media (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_key VARCHAR(255),
    file_type VARCHAR(32), -- image, video
    url VARCHAR(512),
    author_id BIGINT,
    status INT DEFAULT 0, -- 0: PROCESSING, 1: READY, 2: FAILED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_danmaku (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    time_offset_ms BIGINT NOT NULL,
    color VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_danmaku_video_id ON t_danmaku(video_id);

