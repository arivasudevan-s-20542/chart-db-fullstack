-- V2: Create diagrams table
CREATE TABLE diagrams (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7) DEFAULT '#6366F1',
    
    -- Canvas State (CRITICAL for restoring view)
    canvas_zoom DECIMAL(5,3) DEFAULT 1.000,
    canvas_offset_x DECIMAL(10,2) DEFAULT 0.00,
    canvas_offset_y DECIMAL(10,2) DEFAULT 0.00,
    canvas_width DECIMAL(10,2) DEFAULT 5000.00,
    canvas_height DECIMAL(10,2) DEFAULT 5000.00,
    
    -- Sharing & Access
    is_public BOOLEAN DEFAULT false,
    public_slug VARCHAR(100) UNIQUE,
    allow_comments BOOLEAN DEFAULT true,
    allow_anonymous_view BOOLEAN DEFAULT false,
    
    -- Database Configuration (for SQL generation)
    database_type VARCHAR(50) DEFAULT 'postgresql',
    schema_name VARCHAR(100) DEFAULT 'public',
    
    -- Metadata
    tags VARCHAR(2000),
    category VARCHAR(100),
    
    -- Version Control
    current_version_id VARCHAR(36),
    version_number INTEGER DEFAULT 1,
    
    -- Status
    status VARCHAR(20) DEFAULT 'ACTIVE',
    is_template BOOLEAN DEFAULT false,
    
    -- Statistics
    view_count INTEGER DEFAULT 0,
    export_count INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_zoom CHECK (canvas_zoom >= 0.1 AND canvas_zoom <= 10.0),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_diagrams_owner ON diagrams(owner_id);
CREATE INDEX idx_diagrams_public ON diagrams(is_public) WHERE is_public = true;
CREATE INDEX idx_diagrams_slug ON diagrams(public_slug) WHERE public_slug IS NOT NULL;
CREATE INDEX idx_diagrams_status ON diagrams(status);

CREATE TRIGGER update_diagrams_updated_at
    BEFORE UPDATE ON diagrams
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
