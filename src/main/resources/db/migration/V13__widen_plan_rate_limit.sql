-- V13: keep the database type aligned with Plan.rateLimitPerMinute (Java long).
-- This is a widening conversion and preserves all existing rate-limit values.
ALTER TABLE plans
    ALTER COLUMN rate_limit_per_minute TYPE BIGINT
    USING rate_limit_per_minute::BIGINT;
