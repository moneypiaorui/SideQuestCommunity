-- Seed: users, roles, permissions
INSERT INTO t_user (username, password, nickname, avatar, signature, role, status, follower_count, following_count, total_liked_count, post_count)
VALUES
    ('admin', '$2b$12$qhVSGDjFxzI.bnlbf5gT3e.AcPA.snhkXSBNF7aA/Mmu4Kp3pnZfu', 'Admin', '', '', 'ADMIN', 0, 0, 0, 0, 0),
    ('alice', '$2b$12$thJJqsEdmcTd31cMdOyugej../Qnk99Gof6Aju40Mz9LrvbRm8Iym', 'Alice', '', '', 'USER', 0, 0, 0, 0, 0),
    ('bob', '$2b$12$vSTACfyYRQSJz26VH2x2NOautjgGyRfAHHhXYo9zFl2nqo3k9bWyC', 'Bob', '', '', 'USER', 0, 0, 0, 0, 0)
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

