-- V5: Create relationships table
CREATE TABLE relationships (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    
    -- Source and Target
    source_table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    target_table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    source_column_id VARCHAR(36) REFERENCES columns(id) ON DELETE SET NULL,
    target_column_id VARCHAR(36) REFERENCES columns(id) ON DELETE SET NULL,
    
    -- Relationship Type
    relationship_type VARCHAR(50) NOT NULL,
    cardinality_source VARCHAR(10) DEFAULT '1',
    cardinality_target VARCHAR(10) DEFAULT '*',
    
    -- Naming
    name VARCHAR(255),
    constraint_name VARCHAR(255),
    
    -- Visual/Path
    path_type VARCHAR(20) DEFAULT 'auto',
    path_points JSONB,
    source_handle VARCHAR(20) DEFAULT 'right',
    target_handle VARCHAR(20) DEFAULT 'left',
    
    -- Styling
    line_color VARCHAR(7) DEFAULT '#94A3B8',
    line_width DECIMAL(3,1) DEFAULT 2.0,
    line_style VARCHAR(20) DEFAULT 'solid',
    
    -- Labels
    label_source VARCHAR(100),
    label_target VARCHAR(100),
    show_labels BOOLEAN DEFAULT true,
    
    -- Referential Actions
    on_delete VARCHAR(20) DEFAULT 'NO ACTION',
    on_update VARCHAR(20) DEFAULT 'NO ACTION',
    
    -- Flags
    is_identifying BOOLEAN DEFAULT false,
    is_virtual BOOLEAN DEFAULT false,
    
    -- Ordering
    z_index INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_relationship_type CHECK (
        relationship_type IN ('ONE_TO_ONE', 'ONE_TO_MANY', 'MANY_TO_ONE', 'MANY_TO_MANY')
    ),
    CONSTRAINT no_self_relationship CHECK (source_table_id != target_table_id OR source_column_id != target_column_id)
);

CREATE INDEX idx_relationships_diagram ON relationships(diagram_id);
CREATE INDEX idx_relationships_source ON relationships(source_table_id);
CREATE INDEX idx_relationships_target ON relationships(target_table_id);

CREATE TRIGGER update_relationships_updated_at
    BEFORE UPDATE ON relationships
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
