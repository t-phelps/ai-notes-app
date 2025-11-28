CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL primary key,
    customer_id VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    start_date BIGINT NOT NULL,
    created BIGINT NOT NULL,
    current_period_start BIGINT NOT NULL,
    current_period_end BIGINT NOT NULL,
    price_id VARCHAR(255) NOT NULL
);

