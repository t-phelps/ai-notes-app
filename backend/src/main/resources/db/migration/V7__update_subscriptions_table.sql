-- this change is due the async behavior of Stripe webhook events sending partial data
ALTER TABLE subscriptions
    ALTER COLUMN subscription_id DROP NOT NULL,
    ALTER COLUMN status DROP NOT NULL,
    ALTER COLUMN start_date DROP NOT NULL,
    ALTER COLUMN created DROP NOT NULL,
    ALTER COLUMN current_period_start DROP NOT NULL,
    ALTER COLUMN current_period_end DROP NOT NULL,
    ALTER COLUMN price_id DROP NOT NULL;

-- this column is for the upsert conflict
ALTER TABLE subscriptions
    ADD COLUMN latest_invoice VARCHAR(255) UNIQUE;