-- ============================================================================
-- V11: Billing foundations
--
-- Plan definitions are seeded before subscriptions receive plan_id values.
-- The legacy subscriptions.plan column is intentionally retained for this
-- migration so existing application versions remain readable during rollout.
-- It can be removed in a later migration after the Plan-backed entity is live.
-- ============================================================================

CREATE TABLE IF NOT EXISTS plans (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(64)  NOT NULL,
    slug                  VARCHAR(32)  NOT NULL UNIQUE,
    storage_limit_bytes   BIGINT       NOT NULL,
    max_file_size_bytes   BIGINT       NOT NULL,
    max_teams             INTEGER      NOT NULL,
    max_team_members      INTEGER      NOT NULL,
    ai_queries_per_month  INTEGER      NOT NULL,
    rate_limit_per_minute INTEGER      NOT NULL,
    price_cents            INTEGER      NOT NULL,
    currency              VARCHAR(3)   NOT NULL DEFAULT 'USD',
    billing_interval      VARCHAR(16)  NOT NULL DEFAULT 'MONTH',
    stripe_price_id       VARCHAR(255),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plans_stripe_price_id
    ON plans(stripe_price_id)
    WHERE stripe_price_id IS NOT NULL;

INSERT INTO plans (
    name, slug, storage_limit_bytes, max_file_size_bytes, max_teams,
    max_team_members, ai_queries_per_month, rate_limit_per_minute,
    price_cents, currency, billing_interval, active
)
VALUES
    ('Free', 'FREE', 5368709120, 26214400, 1, 3, 10, 100, 0, 'USD', 'MONTH', TRUE),
    ('Pro', 'PRO', 53687091200, 104857600, 5, 10, 200, 500, 999, 'USD', 'MONTH', TRUE),
    ('Business', 'BUSINESS', 1099511627776, 524288000, -1, 50, -1, 2000, 2999, 'USD', 'MONTH', TRUE)
ON CONFLICT (slug) DO NOTHING;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS plan_id BIGINT;

UPDATE subscriptions s
   SET plan_id = p.id
  FROM plans p
 WHERE p.slug = UPPER(s.plan)
   AND s.plan_id IS NULL;

UPDATE subscriptions
   SET plan_id = (SELECT id FROM plans WHERE slug = 'FREE')
 WHERE plan_id IS NULL;

ALTER TABLE subscriptions
    ALTER COLUMN plan_id SET NOT NULL;

ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscriptions_plan
    FOREIGN KEY (plan_id) REFERENCES plans(id);

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(16) NOT NULL DEFAULT 'MONTH',
    ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS current_period_start TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS current_period_end TIMESTAMP(6);

CREATE INDEX IF NOT EXISTS idx_subscriptions_plan_id
    ON subscriptions(plan_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_stripe_customer_id
    ON subscriptions(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_stripe_subscription_id
    ON subscriptions(stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS payments (
    id                       BIGSERIAL PRIMARY KEY,
    user_email               VARCHAR(255) NOT NULL,
    subscription_id          BIGINT       NOT NULL REFERENCES subscriptions(id),
    stripe_payment_intent_id VARCHAR(255),
    stripe_invoice_id        VARCHAR(255),
    amount_cents             INTEGER      NOT NULL,
    currency                 VARCHAR(3)   NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    created_at               TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_stripe_payment_intent_id
    ON payments(stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_stripe_invoice_id
    ON payments(stripe_invoice_id)
    WHERE stripe_invoice_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_user_created_at
    ON payments(user_email, created_at);

CREATE TABLE IF NOT EXISTS webhook_events (
    id              BIGSERIAL PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type      VARCHAR(128) NOT NULL,
    payload         TEXT         NOT NULL,
    processed        BOOLEAN      NOT NULL DEFAULT FALSE,
    processing_error TEXT,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_type_created_at
    ON webhook_events(event_type, created_at);

CREATE TABLE IF NOT EXISTS usage_tracking (
    id            BIGSERIAL PRIMARY KEY,
    user_email    VARCHAR(255) NOT NULL,
    resource_type VARCHAR(32)  NOT NULL,
    period_start  DATE         NOT NULL,
    period_end    DATE         NOT NULL,
    usage_count   INTEGER      NOT NULL DEFAULT 0,
    UNIQUE (user_email, resource_type, period_start)
);

CREATE INDEX IF NOT EXISTS idx_usage_tracking_user_period
    ON usage_tracking(user_email, period_start, period_end);
