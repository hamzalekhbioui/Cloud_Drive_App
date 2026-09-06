-- V12: explicit subscription lifecycle semantics

UPDATE subscriptions
   SET status = UPPER(status)
 WHERE status IS NOT NULL;

ALTER TABLE subscriptions
    ADD CONSTRAINT ck_subscriptions_status
    CHECK (status IN (
        'ACTIVE', 'PAST_DUE', 'CANCELLED', 'INCOMPLETE',
        'INCOMPLETE_EXPIRED', 'TRIALING', 'UNPAID', 'PAUSED'
    ));

ALTER TABLE subscriptions
    ADD CONSTRAINT ck_subscription_period
    CHECK (
        current_period_start IS NULL
        OR current_period_end IS NULL
        OR current_period_end >= current_period_start
    );
