-- Enforce 1-day expiration for all existing active shares that don't have one
UPDATE file_shares 
SET expires_at = created_at + INTERVAL '1 day'
WHERE expires_at IS NULL;

-- Also cap existing expirations that are further than 1 day from creation (optional but consistent)
UPDATE file_shares
SET expires_at = created_at + INTERVAL '1 day'
WHERE expires_at > created_at + INTERVAL '1 day';
