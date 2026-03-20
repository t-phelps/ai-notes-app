CREATE TABLE stripe_events(
    event_id TEXT PRIMARY KEY NOT NULL,
    event_type TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    payload JSONB
);