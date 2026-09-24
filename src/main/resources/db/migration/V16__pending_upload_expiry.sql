ALTER TABLE files
    ADD COLUMN IF NOT EXISTS upload_expires_at TIMESTAMP(6);

UPDATE files
   SET upload_expires_at = created_at + INTERVAL '10 minutes'
 WHERE status = 'PENDING'
   AND upload_expires_at IS NULL
   AND created_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_files_pending_expiry
    ON files(status, upload_expires_at)
    WHERE status = 'PENDING';
