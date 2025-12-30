-- Recalculate counters to keep seed data consistent
UPDATE t_post p
SET like_count = COALESCE(l.cnt, 0)
FROM (
    SELECT post_id, COUNT(*) cnt
    FROM t_like
    GROUP BY post_id
) l
WHERE p.id = l.post_id;

UPDATE t_post
SET like_count = 0
WHERE id NOT IN (SELECT post_id FROM t_like);

UPDATE t_post p
SET comment_count = COALESCE(c.cnt, 0)
FROM (
    SELECT post_id, COUNT(*) cnt
    FROM t_comment
    GROUP BY post_id
) c
WHERE p.id = c.post_id;

UPDATE t_post
SET comment_count = 0
WHERE id NOT IN (SELECT post_id FROM t_comment);

UPDATE t_post p
SET favorite_count = COALESCE(f.cnt, 0)
FROM (
    SELECT post_id, COUNT(*) cnt
    FROM t_favorite
    GROUP BY post_id
) f
WHERE p.id = f.post_id;

UPDATE t_post
SET favorite_count = 0
WHERE id NOT IN (SELECT post_id FROM t_favorite);

UPDATE t_user u
SET follower_count = COALESCE(f.cnt, 0)
FROM (
    SELECT following_id AS user_id, COUNT(*) cnt
    FROM t_follow
    GROUP BY following_id
) f
WHERE u.id = f.user_id;

UPDATE t_user
SET follower_count = 0
WHERE id NOT IN (SELECT following_id FROM t_follow);

UPDATE t_user u
SET following_count = COALESCE(f.cnt, 0)
FROM (
    SELECT follower_id AS user_id, COUNT(*) cnt
    FROM t_follow
    GROUP BY follower_id
) f
WHERE u.id = f.user_id;

UPDATE t_user
SET following_count = 0
WHERE id NOT IN (SELECT follower_id FROM t_follow);

UPDATE t_user u
SET post_count = COALESCE(p.cnt, 0)
FROM (
    SELECT author_id AS user_id, COUNT(*) cnt
    FROM t_post
    GROUP BY author_id
) p
WHERE u.id = p.user_id;

UPDATE t_user
SET post_count = 0
WHERE id NOT IN (SELECT author_id FROM t_post);

UPDATE t_user u
SET total_liked_count = COALESCE(l.cnt, 0)
FROM (
    SELECT p.author_id AS user_id, COUNT(*) cnt
    FROM t_like l
    JOIN t_post p ON p.id = l.post_id
    GROUP BY p.author_id
) l
WHERE u.id = l.user_id;

UPDATE t_user
SET total_liked_count = 0
WHERE id NOT IN (
    SELECT p.author_id
    FROM t_like l
    JOIN t_post p ON p.id = l.post_id
);

