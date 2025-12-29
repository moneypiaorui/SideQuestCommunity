CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    signature VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 0,
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

INSERT INTO t_user (username, password, nickname, avatar, signature, role, status, follower_count, following_count, total_liked_count, post_count)
VALUES
    ('admin', '$2b$12$qhVSGDjFxzI.bnlbf5gT3e.AcPA.snhkXSBNF7aA/Mmu4Kp3pnZfu', 'Admin', '', '', 'ADMIN', 0, 0, 0, 0, 0)
ON CONFLICT (username) DO NOTHING;
