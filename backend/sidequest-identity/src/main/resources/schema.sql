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

CREATE TABLE IF NOT EXISTS t_role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(64),
    description VARCHAR(255),
    status INT DEFAULT 0,
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
    type VARCHAR(32) NOT NULL,
    content TEXT,
    status INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user_id ON t_notification(user_id);

INSERT INTO t_user (username, password, nickname, avatar, signature, role, status, follower_count, following_count, total_liked_count, post_count)
VALUES
    ('admin', '$2b$12$qhVSGDjFxzI.bnlbf5gT3e.AcPA.snhkXSBNF7aA/Mmu4Kp3pnZfu', 'Admin', '', '', 'ADMIN', 0, 0, 0, 0, 0)
ON CONFLICT (username) DO NOTHING;

INSERT INTO t_role (code, name, description, status)
VALUES
    ('ADMIN', 'Administrator', 'System administrator', 0),
    ('USER', 'User', 'Default user role', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO t_permission (code, name, description, resource, action)
VALUES
    ('USER_LIST', 'List Users', 'View user list', 'identity.user', 'list'),
    ('USER_BAN', 'Ban User', 'Ban a user', 'identity.user', 'ban'),
    ('USER_ROLE_ASSIGN', 'Assign User Role', 'Assign roles to user', 'identity.user', 'assign_role'),
    ('ROLE_LIST', 'List Roles', 'View role list', 'identity.role', 'list'),
    ('ROLE_CREATE', 'Create Role', 'Create new role', 'identity.role', 'create'),
    ('ROLE_PERMISSION_ASSIGN', 'Assign Role Permission', 'Assign permissions to role', 'identity.role', 'assign_permission'),
    ('PERMISSION_LIST', 'List Permissions', 'View permission list', 'identity.permission', 'list'),
    ('PERMISSION_CREATE', 'Create Permission', 'Create new permission', 'identity.permission', 'create')
ON CONFLICT (code) DO NOTHING;

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r, t_permission p
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u, t_role r
WHERE r.code = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
