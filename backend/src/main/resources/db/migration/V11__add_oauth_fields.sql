-- V11: Add OAuth2 provider fields to users table
-- Support for Google, GitHub, and Zoho IAM authentication

-- Add OAuth provider column (google, github, zoho, or null for email/password)
ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(20);

-- Add OAuth provider ID (unique ID from the OAuth provider)
ALTER TABLE users ADD COLUMN oauth_provider_id VARCHAR(255);

-- Make password nullable for OAuth users
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Add composite index for OAuth lookup
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_provider_id);

-- Add check constraint for OAuth provider values
ALTER TABLE users ADD CONSTRAINT chk_oauth_provider 
    CHECK (oauth_provider IS NULL OR oauth_provider IN ('google', 'github', 'zoho'));
