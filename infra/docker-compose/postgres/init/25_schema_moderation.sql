-- Schema: moderation
CREATE TABLE IF NOT EXISTS t_sensitive_word (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(128) NOT NULL,
    level VARCHAR(8) DEFAULT 'S1',
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_moderation_case (
    id BIGSERIAL PRIMARY KEY,
    content_type VARCHAR(16) NOT NULL,
    content TEXT,
    content_url VARCHAR(512),
    result VARCHAR(16),
    level VARCHAR(8),
    reason TEXT,
    status INT DEFAULT 0,
    operator_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_report (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    reporter_id BIGINT,
    reason TEXT,
    status INT DEFAULT 0,
    handler_id BIGINT,
    handle_result TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sensitive_word_status ON t_sensitive_word(status);
CREATE INDEX IF NOT EXISTS idx_moderation_case_status ON t_moderation_case(status);
CREATE INDEX IF NOT EXISTS idx_report_status ON t_report(status);
