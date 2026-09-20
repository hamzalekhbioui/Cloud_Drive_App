-- Admin panel: isolated admin identity store + audit trail
CREATE TABLE IF NOT EXISTS admin_users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP(6),
    last_login  TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id           BIGSERIAL PRIMARY KEY,
    admin_id     BIGINT,
    admin_email  VARCHAR(255) NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    target_type  VARCHAR(64),
    target_id    VARCHAR(255),
    detail       TEXT,
    ip_address   VARCHAR(64),
    created_at   TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_created_at  ON admin_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_admin_email ON admin_audit_log(admin_email);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_action      ON admin_audit_log(action);

-- Account lifecycle + stateless force-logout support
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS tokens_valid_from TIMESTAMP(6);

-- Supporting indexes for paged admin list queries
CREATE INDEX IF NOT EXISTS idx_files_created_at        ON files(created_at);
CREATE INDEX IF NOT EXISTS idx_file_shares_revoked_at  ON file_shares(revoked_at);
