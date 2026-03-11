ALTER TABLE subscriptions ALTER COLUMN status DROP NOT NULL;
ALTER TABLE subscriptions ALTER COLUMN current_period_end DROP NOT NULL;