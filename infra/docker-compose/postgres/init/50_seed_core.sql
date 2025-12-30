-- Seed: sections and tags
INSERT INTO t_section (name, display_name_zh, display_name_en, status)
VALUES
    ('rpg', '角色扮演', 'RPG', 0),
    ('fps', '射击', 'FPS', 0),
    ('moba', 'MOBA', 'MOBA', 0),
    ('strategy', '策略', 'Strategy', 0),
    ('indie', '独立游戏', 'Indie', 0),
    ('esports', '电竞', 'Esports', 0),
    ('console', '主机', 'Console', 0),
    ('pc', 'PC', 'PC', 0),
    ('mobile', '手游', 'Mobile', 0)
ON CONFLICT (name) DO UPDATE SET
    display_name_zh = EXCLUDED.display_name_zh,
    display_name_en = EXCLUDED.display_name_en,
    status = EXCLUDED.status;

INSERT INTO t_tag (name, hit_count)
VALUES
    ('build', 12),
    ('guide', 5),
    ('balance', 8),
    ('patch', 9),
    ('pvp', 7),
    ('pve', 6),
    ('coop', 4),
    ('ranked', 10),
    ('meta', 11),
    ('cosplay', 3)
ON CONFLICT (name) DO UPDATE SET
    hit_count = EXCLUDED.hit_count;

