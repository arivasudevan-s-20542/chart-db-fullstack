-- V9: Create audit_logs table
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    
    -- Context
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    diagram_id VARCHAR(36) REFERENCES diagrams(id) ON DELETE CASCADE,
    session_id VARCHAR(100),
    
    -- Action
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36),
    entity_name VARCHAR(255),
    
    -- Changes
    old_value JSONB,
    new_value JSONB,
    changes JSONB,
    
    -- Request Info
    ip_address INET,
    user_agent TEXT,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_action_type CHECK (
        action_type IN ('CREATE', 'UPDATE', 'DELETE', 'VIEW', 'EXPORT', 'SHARE', 'LOGIN', 'LOGOUT')
    )
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_diagram ON audit_logs(diagram_id);
CREATE INDEX idx_audit_action ON audit_logs(action_type);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_logs(created_at);
