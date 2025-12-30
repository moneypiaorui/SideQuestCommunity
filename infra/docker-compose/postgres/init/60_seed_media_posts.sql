-- Seed: media and posts
INSERT INTO t_media (file_name, file_key, file_type, url, author_id, status)
SELECT 'rpg_cover.jpg', 'media/rpg_cover.jpg', 'image', 'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80', u.id, 1
FROM t_user u WHERE u.username = 'alice'
AND NOT EXISTS (SELECT 1 FROM t_media WHERE file_key = 'media/rpg_cover.jpg');

INSERT INTO t_media (file_name, file_key, file_type, url, author_id, status)
SELECT 'fps_clip.mp4', 'media/fps_clip.mp4', 'video', 'https://samplelib.com/lib/preview/mp4/sample-5s.mp4', u.id, 1
FROM t_user u WHERE u.username = 'bob'
AND NOT EXISTS (SELECT 1 FROM t_media WHERE file_key = 'media/fps_clip.mp4');

INSERT INTO t_post (author_id, title, content, section_id, status, like_count, comment_count, favorite_count, view_count, image_urls, video_url, video_cover_url, video_duration, media_id)
SELECT u.id,
       'RPG build notes',
       'Best early game build tips for new players.',
       s.id,
       1, 3, 2, 1, 120,
       'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80',
       NULL, NULL, 0,
       m.id
FROM t_user u, t_section s, t_media m
WHERE u.username = 'alice' AND s.name = 'rpg' AND m.file_key = 'media/rpg_cover.jpg'
AND NOT EXISTS (SELECT 1 FROM t_post WHERE title = 'RPG build notes');

INSERT INTO t_post (author_id, title, content, section_id, status, like_count, comment_count, favorite_count, view_count, image_urls, video_url, video_cover_url, video_duration, media_id)
SELECT u.id,
       'FPS aim training routine',
       'A 15-minute daily routine to improve aim.',
       s.id,
       1, 5, 1, 2, 240,
       NULL,
       'https://samplelib.com/lib/preview/mp4/sample-5s.mp4',
       'https://images.unsplash.com/photo-1513941023128-3b4b7df2a3dd?auto=format&fit=crop&w=1200&q=80',
       90,
       m.id
FROM t_user u, t_section s, t_media m
WHERE u.username = 'bob' AND s.name = 'fps' AND m.file_key = 'media/fps_clip.mp4'
AND NOT EXISTS (SELECT 1 FROM t_post WHERE title = 'FPS aim training routine');

INSERT INTO t_comment (post_id, user_id, content, parent_id)
SELECT p.id, u.id, 'Great tips, thanks!', NULL
FROM t_post p, t_user u
WHERE p.title = 'RPG build notes' AND u.username = 'bob'
AND NOT EXISTS (SELECT 1 FROM t_comment WHERE content = 'Great tips, thanks!' AND user_id = u.id);

INSERT INTO t_comment (post_id, user_id, content, parent_id)
SELECT p.id, u.id, 'Will try this routine today.', NULL
FROM t_post p, t_user u
WHERE p.title = 'FPS aim training routine' AND u.username = 'alice'
AND NOT EXISTS (SELECT 1 FROM t_comment WHERE content = 'Will try this routine today.' AND user_id = u.id);

INSERT INTO t_like (post_id, user_id)
SELECT p.id, u.id
FROM t_post p, t_user u
WHERE p.title = 'RPG build notes' AND u.username = 'bob'
ON CONFLICT (post_id, user_id) DO NOTHING;

INSERT INTO t_favorite (post_id, user_id, collection_id)
SELECT p.id, u.id, 1
FROM t_post p, t_user u
WHERE p.title = 'FPS aim training routine' AND u.username = 'alice'
AND NOT EXISTS (SELECT 1 FROM t_favorite WHERE post_id = p.id AND user_id = u.id);

INSERT INTO t_post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM t_post p, t_tag t
WHERE p.title = 'RPG build notes' AND t.name IN ('build', 'pve')
ON CONFLICT (post_id, tag_id) DO NOTHING;

INSERT INTO t_post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM t_post p, t_tag t
WHERE p.title = 'FPS aim training routine' AND t.name IN ('guide', 'pvp')
ON CONFLICT (post_id, tag_id) DO NOTHING;

INSERT INTO t_danmaku (video_id, user_id, content, time_offset_ms, color)
SELECT p.id, u.id, 'Nice clip!', 12000, '#ffffff'
FROM t_post p, t_user u
WHERE p.title = 'FPS aim training routine' AND u.username = 'alice'
AND NOT EXISTS (SELECT 1 FROM t_danmaku WHERE video_id = p.id AND user_id = u.id AND content = 'Nice clip!');

