-- V8: Create diagram_versions table for version history
CREATE TABLE diagram_versions (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    
    -- Version Info
    version_number INTEGER NOT NULL,
    version_label VARCHAR(100),
    description TEXT,
    
    -- Snapshot (complete diagram state)
    snapshot_data JSONB NOT NULL,
    
    -- Change Tracking
    changes_summary TEXT,
    changes_detail JSONB,
    
    -- Creator
    created_by VARCHAR(36) REFERENCES users(id),
    
    -- Status
    is_current BOOLEAN DEFAULT false,
    is_auto_save BOOLEAN DEFAULT false,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_version_number UNIQUE (diagram_id, version_number)
);

CREATE INDEX idx_versions_diagram ON diagram_versions(diagram_id);
CREATE INDEX idx_versions_current ON diagram_versions(diagram_id, is_current) WHERE is_current = true;
CREATE INDEX idx_versions_created ON diagram_versions(diagram_id, created_at);
