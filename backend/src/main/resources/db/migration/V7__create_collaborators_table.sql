-- V7: Create active_collaborators table for real-time presence tracking
CREATE TABLE active_collaborators (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Session Info
    session_id VARCHAR(100) NOT NULL,
    websocket_session_id VARCHAR(100),
    
    -- Cursor Position
    cursor_x DECIMAL(10,2),
    cursor_y DECIMAL(10,2),
    cursor_color VARCHAR(7),
    
    -- User Info (denormalized for performance)
    user_name VARCHAR(100),
    user_avatar VARCHAR(500),
    
    -- Selection
    selected_table_id VARCHAR(36) REFERENCES tables(id) ON DELETE SET NULL,
    selected_column_id VARCHAR(36) REFERENCES columns(id) ON DELETE SET NULL,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_idle BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'active',
    
    -- Device Info
    device_type VARCHAR(20),
    browser VARCHAR(50),
    
    -- Timestamps
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_session UNIQUE (diagram_id, user_id, session_id)
);

CREATE INDEX idx_collaborators_diagram ON active_collaborators(diagram_id);
CREATE INDEX idx_collaborators_active ON active_collaborators(diagram_id, is_active) WHERE is_active = true;
CREATE INDEX idx_collaborators_session ON active_collaborators(session_id);
CREATE INDEX idx_collaborators_stale ON active_collaborators(last_seen) WHERE is_active = true;
