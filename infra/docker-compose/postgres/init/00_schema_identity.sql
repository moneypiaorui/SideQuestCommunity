-- Schema: identity
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

CREATE TABLE IF NOT EXISTS t_role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(64),
    description VARCHAR(255),
    status INT DEFAULT 0, -- 0: ACTIVE, 1: DISABLED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_permission (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(128) UNIQUE NOT NULL,
    name VARCHAR(64),
    description VARCHAR(255),
    resource VARCHAR(128),
    action VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON t_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON t_user_role(role_id);
CREATE INDEX IF NOT EXISTS idx_role_perm_role_id ON t_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_perm_perm_id ON t_role_permission(permission_id);

CREATE TABLE IF NOT EXISTS t_ban_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255),
    operator_id BIGINT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL, -- chat/interaction/system
    content TEXT,
    status INT DEFAULT 0, -- 0: unread, 1: read
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user_id ON t_notification(user_id);

