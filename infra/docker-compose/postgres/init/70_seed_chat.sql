-- Seed: chat
INSERT INTO t_chat_room (name, type)
SELECT 'RPG Squad', 'GROUP'
WHERE NOT EXISTS (SELECT 1 FROM t_chat_room WHERE name = 'RPG Squad');

INSERT INTO t_chat_room (name, type)
SELECT 'Aim Lab', 'GROUP'
WHERE NOT EXISTS (SELECT 1 FROM t_chat_room WHERE name = 'Aim Lab');

INSERT INTO t_chat_room_member (room_id, user_id, last_read_message_id)
SELECT r.id, u.id, 0
FROM t_chat_room r, t_user u
WHERE r.name = 'RPG Squad' AND u.username IN ('admin', 'alice')
ON CONFLICT (room_id, user_id) DO NOTHING;

INSERT INTO t_chat_room_member (room_id, user_id, last_read_message_id)
SELECT r.id, u.id, 0
FROM t_chat_room r, t_user u
WHERE r.name = 'Aim Lab' AND u.username IN ('admin', 'bob')
ON CONFLICT (room_id, user_id) DO NOTHING;

INSERT INTO t_chat_message (room_id, sender_id, content, type, status)
SELECT r.id, u.id, 'Welcome to RPG Squad!', 'TEXT', 0
FROM t_chat_room r, t_user u
WHERE r.name = 'RPG Squad' AND u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM t_chat_message WHERE content = 'Welcome to RPG Squad!');

INSERT INTO t_chat_message (room_id, sender_id, content, type, status)
SELECT r.id, u.id, 'Share your aim routines here.', 'TEXT', 0
FROM t_chat_room r, t_user u
WHERE r.name = 'Aim Lab' AND u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM t_chat_message WHERE content = 'Share your aim routines here.');

