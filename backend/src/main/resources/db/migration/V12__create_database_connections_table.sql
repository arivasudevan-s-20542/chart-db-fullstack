-- V12: Create database connections table for live database linking
CREATE TABLE database_connections (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Connection details
    name VARCHAR(100) NOT NULL,
    database_type VARCHAR(50) NOT NULL, -- postgresql, mysql, sqlserver, oracle, mongodb
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    encrypted_password TEXT NOT NULL, -- AES-256 encrypted
    
    -- Additional settings
    ssl_enabled BOOLEAN DEFAULT true,
    additional_params JSONB,
    
    -- Status tracking
    status VARCHAR(20) DEFAULT 'UNKNOWN', -- CONNECTED, ERROR, UNKNOWN
    last_connected_at TIMESTAMP,
    last_error TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_status CHECK (status IN ('CONNECTED', 'ERROR', 'UNKNOWN'))
);

CREATE INDEX idx_database_connections_diagram ON database_connections(diagram_id);
CREATE INDEX idx_database_connections_user ON database_connections(user_id);

CREATE TRIGGER update_database_connections_updated_at
    BEFORE UPDATE ON database_connections
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
