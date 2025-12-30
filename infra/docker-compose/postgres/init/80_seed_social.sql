-- Seed: follow relations
INSERT INTO t_follow (follower_id, following_id)
SELECT u1.id, u2.id
FROM t_user u1, t_user u2
WHERE u1.username = 'alice' AND u2.username = 'bob'
ON CONFLICT (follower_id, following_id) DO NOTHING;

INSERT INTO t_follow (follower_id, following_id)
SELECT u1.id, u2.id
FROM t_user u1, t_user u2
WHERE u1.username = 'bob' AND u2.username = 'alice'
ON CONFLICT (follower_id, following_id) DO NOTHING;

INSERT INTO t_follow (follower_id, following_id)
SELECT u1.id, u2.id
FROM t_user u1, t_user u2
WHERE u1.username = 'alice' AND u2.username = 'admin'
ON CONFLICT (follower_id, following_id) DO NOTHING;

