CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6),
    last_login TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS files (
    id BIGSERIAL PRIMARY KEY,
    original_file_name VARCHAR(255),
    blob_file_name VARCHAR(255),
    url VARCHAR(255),
    size BIGINT,
    type VARCHAR(255),
    user_id VARCHAR(255),
    created_at TIMESTAMP(6),
    starred BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS user_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    dark_mode BOOLEAN NOT NULL DEFAULT FALSE,
    density VARCHAR(255),
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    storage_warning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    upload_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    delete_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    default_view VARCHAR(255),
    default_sort VARCHAR(255),
    auto_organize BOOLEAN NOT NULL DEFAULT FALSE,
    debug_mode BOOLEAN NOT NULL DEFAULT FALSE,
    api_token VARCHAR(64),
    updated_at TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_files_user_deleted ON files(user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_files_user_star_deleted ON files(user_id, starred, deleted_at);
