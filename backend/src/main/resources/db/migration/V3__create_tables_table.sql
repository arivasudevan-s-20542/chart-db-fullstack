-- V3: Create tables table (database entities)
-- CRITICAL: position_x and position_y must always be saved!
CREATE TABLE tables (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    
    -- Position (CRITICAL - for canvas layout)
    position_x DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    position_y DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    
    -- Dimensions
    width DECIMAL(10,2) DEFAULT 200.00,
    height DECIMAL(10,2),
    min_width DECIMAL(10,2) DEFAULT 150.00,
    max_width DECIMAL(10,2) DEFAULT 500.00,
    
    -- Styling
    color VARCHAR(7) DEFAULT '#6366F1',
    header_color VARCHAR(7),
    border_color VARCHAR(7),
    border_radius DECIMAL(4,1) DEFAULT 8.0,
    opacity DECIMAL(3,2) DEFAULT 1.00,
    
    -- Flags
    is_collapsed BOOLEAN DEFAULT false,
    is_locked BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,
    
    -- Metadata
    table_type VARCHAR(50) DEFAULT 'table',
    schema_name VARCHAR(100) DEFAULT 'public',
    
    -- Ordering
    z_index INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    
    -- Notes
    notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_position CHECK (
        position_x >= -10000 AND position_x <= 10000 AND
        position_y >= -10000 AND position_y <= 10000
    ),
    CONSTRAINT valid_dimensions CHECK (
        width > 0 AND (height IS NULL OR height > 0)
    ),
    CONSTRAINT unique_table_name_per_diagram UNIQUE (diagram_id, name)
);

CREATE INDEX idx_tables_diagram ON tables(diagram_id);
CREATE INDEX idx_tables_position ON tables(diagram_id, position_x, position_y);
CREATE INDEX idx_tables_name ON tables(diagram_id, name);

CREATE TRIGGER update_tables_updated_at
    BEFORE UPDATE ON tables
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
