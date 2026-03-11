ALTER TABLE subscriptions ALTER COLUMN start_date TYPE TIMESTAMPTZ USING to_timestamp(start_date);

ALTER TABLE subscriptions ALTER COLUMN created TYPE TIMESTAMPTZ USING to_timestamp(created);

ALTER TABLE subscriptions ALTER COLUMN current_period_start TYPE TIMESTAMPTZ USING to_timestamp(current_period_start);

ALTER TABLE subscriptions ALTER COLUMN current_period_end TYPE TIMESTAMPTZ USING to_timestamp(current_period_end);