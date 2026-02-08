-- Create MCP API tokens table for MCP client authentication
CREATE TABLE IF NOT EXISTS mcp_api_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_prefix VARCHAR(12) NOT NULL,
    scopes VARCHAR(500) DEFAULT 'mcp:read,mcp:write',
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_mcp_api_tokens_token_hash ON mcp_api_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_mcp_api_tokens_user_id ON mcp_api_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_mcp_api_tokens_prefix ON mcp_api_tokens(token_prefix);
CREATE INDEX IF NOT EXISTS idx_mcp_api_tokens_active ON mcp_api_tokens(user_id, is_active) WHERE is_active = true;
