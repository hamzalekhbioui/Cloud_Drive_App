CREATE TABLE IF NOT EXISTS file_ai_processing (
    file_id BIGINT PRIMARY KEY REFERENCES files(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    error TEXT,
    summary TEXT,
    processed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_ai_chunks (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding_json TEXT,
    source_metadata TEXT,
    UNIQUE (file_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_file_ai_chunks_file_id ON file_ai_chunks(file_id);

CREATE TABLE IF NOT EXISTS file_ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id VARCHAR(320) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_file_ai_chat_file_user ON file_ai_chat_messages(file_id, user_id, created_at);
