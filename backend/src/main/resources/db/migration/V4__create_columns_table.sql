-- V4: Create columns table (fields within tables)
CREATE TABLE columns (
    id VARCHAR(36) PRIMARY KEY,
    table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    
    -- Data Type
    data_type VARCHAR(100) NOT NULL,
    native_type VARCHAR(100),
    length INTEGER,
    precision INTEGER,
    scale INTEGER,
    
    -- Constraints
    is_primary_key BOOLEAN DEFAULT false,
    is_foreign_key BOOLEAN DEFAULT false,
    is_nullable BOOLEAN DEFAULT true,
    is_unique BOOLEAN DEFAULT false,
    is_auto_increment BOOLEAN DEFAULT false,
    is_indexed BOOLEAN DEFAULT false,
    
    -- Default Value
    default_value TEXT,
    default_expression TEXT,
    
    -- Foreign Key Info (if is_foreign_key = true)
    fk_table_id VARCHAR(36) REFERENCES tables(id) ON DELETE SET NULL,
    fk_column_id VARCHAR(36),
    fk_constraint_name VARCHAR(255),
    fk_on_delete VARCHAR(20) DEFAULT 'NO ACTION',
    fk_on_update VARCHAR(20) DEFAULT 'NO ACTION',
    
    -- Check Constraints
    check_constraint TEXT,
    
    -- Enum Values (if data_type is enum) - stored as comma-separated string
    enum_values VARCHAR(2000),
    
    -- Ordering
    order_index INTEGER NOT NULL DEFAULT 0,
    
    -- Visibility
    is_hidden BOOLEAN DEFAULT false,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_fk_actions CHECK (
        fk_on_delete IN ('NO ACTION', 'CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT') AND
        fk_on_update IN ('NO ACTION', 'CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT')
    ),
    CONSTRAINT unique_column_name_per_table UNIQUE (table_id, name)
);

-- Add self-referencing FK after table creation
ALTER TABLE columns 
    ADD CONSTRAINT fk_column_ref 
    FOREIGN KEY (fk_column_id) REFERENCES columns(id) ON DELETE SET NULL;

CREATE INDEX idx_columns_table ON columns(table_id);
CREATE INDEX idx_columns_order ON columns(table_id, order_index);
CREATE INDEX idx_columns_pk ON columns(table_id) WHERE is_primary_key = true;
CREATE INDEX idx_columns_fk ON columns(table_id) WHERE is_foreign_key = true;

CREATE TRIGGER update_columns_updated_at
    BEFORE UPDATE ON columns
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
