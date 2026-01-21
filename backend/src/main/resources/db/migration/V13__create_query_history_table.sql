-- V13: Create query history table for SQL execution tracking
CREATE TABLE query_history (
    id VARCHAR(36) PRIMARY KEY,
    connection_id VARCHAR(36) REFERENCES database_connections(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Query details
    query TEXT NOT NULL,
    execution_time_ms INTEGER,
    rows_affected INTEGER,
    
    -- Status
    status VARCHAR(20) NOT NULL, -- SUCCESS, ERROR
    error_message TEXT,
    
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_query_status CHECK (status IN ('SUCCESS', 'ERROR'))
);

CREATE INDEX idx_query_history_user ON query_history(user_id, executed_at DESC);
CREATE INDEX idx_query_history_connection ON query_history(connection_id, executed_at DESC);

-- Saved queries table
CREATE TABLE saved_queries (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    name VARCHAR(100) NOT NULL,
    description TEXT,
    query TEXT NOT NULL,
    tags VARCHAR(500),
    is_public BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saved_queries_diagram ON saved_queries(diagram_id);
CREATE INDEX idx_saved_queries_user ON saved_queries(user_id);

CREATE TRIGGER update_saved_queries_updated_at
    BEFORE UPDATE ON saved_queries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
