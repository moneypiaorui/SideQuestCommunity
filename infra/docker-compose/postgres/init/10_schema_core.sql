-- Schema: core
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

CREATE TABLE IF NOT EXISTS t_post_tag (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_post_tag_post_id ON t_post_tag(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tag_tag_id ON t_post_tag(tag_id);

