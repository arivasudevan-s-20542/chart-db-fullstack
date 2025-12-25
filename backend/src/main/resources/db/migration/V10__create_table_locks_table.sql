-- V10: Create table_locks table for optimistic locking
CREATE TABLE table_locks (
    id VARCHAR(36) PRIMARY KEY,
    table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Lock Info
    lock_type VARCHAR(20) DEFAULT 'edit',
    
    -- Expiration (locks auto-expire)
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 seconds'),
    
    -- Constraints
    CONSTRAINT unique_table_lock UNIQUE (table_id)
);

CREATE INDEX idx_locks_table ON table_locks(table_id);
CREATE INDEX idx_locks_expiry ON table_locks(expires_at);

-- Function to clean expired locks
CREATE OR REPLACE FUNCTION cleanup_expired_locks()
RETURNS void AS $$
BEGIN
    DELETE FROM table_locks WHERE expires_at < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;
