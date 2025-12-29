-- SideQuestCommunity PostgreSQL init script
-- Schema for identity, core, media, chat

CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    signature VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 0, -- 0:正常, 1:封禁, 2:删除
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    total_liked_count INT DEFAULT 0,
    post_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_follow (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS t_post (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    section_id BIGINT,
    status INT DEFAULT 0, -- 0:审核中, 1:正常, 2:封禁, 3:删除
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    image_urls TEXT,
    video_url VARCHAR(255),
    video_cover_url VARCHAR(255),
    video_duration INT DEFAULT 0,
    media_id BIGINT,
    tags VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_like (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS t_favorite (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    collection_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_section (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    display_name_zh VARCHAR(64),
    display_name_en VARCHAR(64),
    status INT DEFAULT 0 -- 0: 正常, 1: 隐藏
);

CREATE TABLE IF NOT EXISTS t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    hit_count BIGINT DEFAULT 0
);

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

CREATE TABLE IF NOT EXISTS t_chat_room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64),
    type VARCHAR(20), -- PRIVATE, GROUP
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_chat_room_member (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT DEFAULT 0,
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(room_id, user_id)
);

CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    type VARCHAR(20), -- TEXT, IMAGE, VIDEO
    status INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed data
INSERT INTO t_user (username, password, nickname, avatar, signature, role, status, follower_count, following_count, total_liked_count, post_count)
VALUES
    ('admin', '$2b$12$qhVSGDjFxzI.bnlbf5gT3e.AcPA.snhkXSBNF7aA/Mmu4Kp3pnZfu', 'Admin', '', '', 'ADMIN', 0, 0, 0, 0, 0),
    ('alice', '$2b$12$thJJqsEdmcTd31cMdOyugej../Qnk99Gof6Aju40Mz9LrvbRm8Iym', 'Alice', '', '', 'USER', 0, 12, 5, 20, 2),
    ('bob', '$2b$12$vSTACfyYRQSJz26VH2x2NOautjgGyRfAHHhXYo9zFl2nqo3k9bWyC', 'Bob', '', '', 'USER', 0, 3, 8, 5, 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO t_section (name, display_name_zh, display_name_en, status)
VALUES
    ('design', '设计', 'Design', 0),
    ('ui', 'UI', 'UI', 0),
    ('food', '美食', 'Food', 0)
ON CONFLICT (name) DO NOTHING;

-- INSERT INTO t_post (author_id, title, content, section_id, status, like_count, comment_count, favorite_count, view_count, image_urls, video_url, video_cover_url, video_duration, media_id, tags)
-- VALUES
--     (2, 'Alice', 'First Post', 'Hello SideQuest!', 1, 0, 5, 1, 2, 20, '', '', '', 0, NULL, 'intro,hello'),
--     (3, 'Bob', 'Food Notes', 'Great food nearby.', 3, 0, 2, 0, 1, 10, '', '', '', 0, NULL, 'food,local');

-- INSERT INTO t_comment (post_id, user_id, content, parent_id)
-- VALUES
--     (1, 3, 'Nice post!', NULL);

-- INSERT INTO t_follow (follower_id, following_id)
-- VALUES
--     (2, 3),
--     (3, 2)
-- ON CONFLICT (follower_id, following_id) DO NOTHING;
