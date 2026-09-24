CREATE TABLE IF NOT EXISTS folders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    team_id BIGINT,
    parent_id BIGINT,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

ALTER TABLE files ADD COLUMN IF NOT EXISTS folder_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_folders_user_team_parent ON folders(user_id, team_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_files_folder_id ON files(folder_id);
