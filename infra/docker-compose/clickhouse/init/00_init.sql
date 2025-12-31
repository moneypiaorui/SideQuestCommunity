CREATE TABLE IF NOT EXISTS events (
    event_type String,
    user_id Nullable(UInt64),
    event_time DateTime,
    event_data String,
    created_at DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY (event_type, event_time);
