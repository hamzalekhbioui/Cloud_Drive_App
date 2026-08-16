-- Add status column to support two-phase direct-to-storage uploads.
-- ACTIVE = committed and visible; PENDING = client is still uploading to Azure.
ALTER TABLE files ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

-- Existing rows are already committed uploads, so they get 'ACTIVE' by default.
-- No data back-fill needed.

CREATE INDEX IF NOT EXISTS idx_files_user_status_deleted ON files(user_id, status, deleted_at);
