-- V14: Add status fields to tables for tracking live database state
ALTER TABLE tables 
ADD COLUMN status VARCHAR(20) DEFAULT 'PLANNED',
ADD COLUMN source_database VARCHAR(100),
ADD COLUMN last_verified_at TIMESTAMP,
ADD COLUMN sync_metadata JSONB;

-- Add constraint for status
ALTER TABLE tables
ADD CONSTRAINT valid_table_status CHECK (status IN ('PLANNED', 'EXISTS', 'SYNCED', 'DRIFT', 'DEPRECATED'));

-- Index for filtering by status
CREATE INDEX idx_tables_status ON tables(status);

-- Schema sync status table
CREATE TABLE schema_sync_status (
    diagram_id VARCHAR(36) PRIMARY KEY REFERENCES diagrams(id) ON DELETE CASCADE,
    connection_id VARCHAR(36) REFERENCES database_connections(id) ON DELETE SET NULL,
    
    last_sync_at TIMESTAMP,
    sync_direction VARCHAR(20), -- TO_DB, FROM_DB, TWO_WAY
    tables_synced INTEGER,
    drift_detected BOOLEAN DEFAULT false,
    drift_details JSONB,
    next_auto_sync TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_schema_sync_status_updated_at
    BEFORE UPDATE ON schema_sync_status
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
