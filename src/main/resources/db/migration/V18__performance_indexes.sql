-- Stable, bounded user file listings. Partial indexes avoid indexing PENDING
-- uploads and rows that cannot appear in the corresponding endpoint.
CREATE INDEX IF NOT EXISTS idx_files_user_active_created_id
    ON files(user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_files_user_starred_created_id
    ON files(user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL AND status = 'ACTIVE' AND starred = TRUE;

CREATE INDEX IF NOT EXISTS idx_files_user_trash_deleted_id
    ON files(user_id, deleted_at DESC, id DESC)
    WHERE deleted_at IS NOT NULL AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_files_team_active_created_id
    ON files(team_id, created_at DESC, id DESC)
    WHERE team_id IS NOT NULL AND deleted_at IS NULL AND status = 'ACTIVE';

-- Recipient inbox and admin share expansion paths.
CREATE INDEX IF NOT EXISTS idx_file_shares_recipient_active_created_id
    ON file_shares(LOWER(shared_with_email), created_at DESC, id DESC)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_file_shares_file_created_id
    ON file_shares(file_id, created_at DESC, id DESC);
