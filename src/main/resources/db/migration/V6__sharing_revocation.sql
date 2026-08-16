-- Add revoked_at to file_shares for soft-revocation support
ALTER TABLE file_shares ADD COLUMN revoked_at TIMESTAMP(6);
-- Ensure token column can hold larger Base64 tokens (32 bytes -> ~43-44 chars)
ALTER TABLE file_shares ALTER COLUMN token TYPE VARCHAR(255);
