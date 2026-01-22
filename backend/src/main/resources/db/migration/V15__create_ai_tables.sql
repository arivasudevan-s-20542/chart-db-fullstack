-- V15: Create AI assistant tables for chat, agents, and API configurations

-- AI chat sessions
CREATE TABLE ai_chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    agent_config JSONB NOT NULL,
    context JSONB, -- Snapshot of diagram state
    
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_ai_chat_sessions_diagram ON ai_chat_sessions(diagram_id);
CREATE INDEX idx_ai_chat_sessions_user ON ai_chat_sessions(user_id);
CREATE INDEX idx_ai_chat_sessions_active ON ai_chat_sessions(is_active) WHERE is_active = true;

-- AI messages
CREATE TABLE ai_messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content TEXT NOT NULL,
    metadata JSONB, -- Token count, model used, etc.
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_ai_messages_session ON ai_messages(session_id, created_at);

-- AI suggested changes (for review before applying)
CREATE TABLE ai_suggested_changes (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    message_id VARCHAR(36) REFERENCES ai_messages(id),
    
    changes JSONB NOT NULL, -- Array of change operations
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, PARTIAL
    applied_changes JSONB, -- What was actually applied
    
    applied_at TIMESTAMP,
    applied_by VARCHAR(36) REFERENCES users(id),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_change_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'PARTIAL'))
);

CREATE INDEX idx_ai_suggested_changes_session ON ai_suggested_changes(session_id);
CREATE INDEX idx_ai_suggested_changes_pending ON ai_suggested_changes(session_id, status) WHERE status = 'PENDING';

-- User AI configurations (API keys, preferences)
CREATE TABLE user_ai_configs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    config JSONB NOT NULL, -- Encrypted API keys and settings
    usage_stats JSONB, -- Token usage tracking
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_user_ai_configs_updated_at
    BEFORE UPDATE ON user_ai_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- AI agents (built-in + custom)
CREATE TABLE ai_agents (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE, -- NULL for system agents
    
    name VARCHAR(100) NOT NULL,
    description TEXT,
    config JSONB NOT NULL, -- AgentConfig (capabilities, model, prompt, etc.)
    
    is_public BOOLEAN DEFAULT false,
    is_system BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_agents_user ON ai_agents(user_id);
CREATE INDEX idx_ai_agents_public ON ai_agents(is_public) WHERE is_public = true;
CREATE INDEX idx_ai_agents_system ON ai_agents(is_system) WHERE is_system = true;

CREATE TRIGGER update_ai_agents_updated_at
    BEFORE UPDATE ON ai_agents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- MCP server configurations
CREATE TABLE mcp_servers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    name VARCHAR(100) NOT NULL,
    config JSONB NOT NULL, -- MCPServerConfig
    
    is_active BOOLEAN DEFAULT true,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcp_servers_user ON mcp_servers(user_id);
CREATE INDEX idx_mcp_servers_active ON mcp_servers(is_active) WHERE is_active = true;

CREATE TRIGGER update_mcp_servers_updated_at
    BEFORE UPDATE ON mcp_servers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
