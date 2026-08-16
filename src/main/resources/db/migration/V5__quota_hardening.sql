-- ============================================================================
-- V5: Quota hardening — add a counter column and optimistic-lock version
--
-- used_bytes  : tracks cumulative storage consumption (maintained atomically
--               by SubscriptionService under PESSIMISTIC_WRITE lock)
-- version     : JPA @Version for optimistic locking / dirty-check safety
-- ============================================================================

ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS used_bytes BIGINT NOT NULL DEFAULT 0;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS version   BIGINT NOT NULL DEFAULT 0;

-- Seed used_bytes from the actual file table so existing rows are accurate.
UPDATE subscriptions s
   SET used_bytes = COALESCE(
       (SELECT SUM(f.size) FROM files f
         WHERE f.user_id = s.user_email
           AND f.deleted_at IS NULL
           AND f.status = 'ACTIVE'),
       0);
