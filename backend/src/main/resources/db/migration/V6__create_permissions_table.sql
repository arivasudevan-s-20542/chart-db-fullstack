-- V6: Create diagram_permissions table
CREATE TABLE diagram_permissions (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    
    -- For team/email invitations
    invited_email VARCHAR(255),
    
    -- Permission Level
    permission_level VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    
    -- Granular Permissions
    can_view BOOLEAN DEFAULT true,
    can_edit BOOLEAN DEFAULT false,
    can_comment BOOLEAN DEFAULT false,
    can_share BOOLEAN DEFAULT false,
    can_export BOOLEAN DEFAULT true,
    can_delete BOOLEAN DEFAULT false,
    can_manage_permissions BOOLEAN DEFAULT false,
    
    -- Invitation Status
    invitation_status VARCHAR(20) DEFAULT 'accepted',
    invited_by VARCHAR(36) REFERENCES users(id),
    invited_at TIMESTAMP,
    accepted_at TIMESTAMP,
    
    -- Expiration
    expires_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_permission_level CHECK (
        permission_level IN ('OWNER', 'EDITOR', 'COMMENTER', 'VIEWER')
    ),
    CONSTRAINT user_or_email CHECK (user_id IS NOT NULL OR invited_email IS NOT NULL),
    CONSTRAINT unique_user_diagram UNIQUE (diagram_id, user_id),
    CONSTRAINT unique_email_diagram UNIQUE (diagram_id, invited_email)
);

CREATE INDEX idx_permissions_diagram ON diagram_permissions(diagram_id);
CREATE INDEX idx_permissions_user ON diagram_permissions(user_id);
CREATE INDEX idx_permissions_email ON diagram_permissions(invited_email) WHERE invited_email IS NOT NULL;

CREATE TRIGGER update_permissions_updated_at
    BEFORE UPDATE ON diagram_permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
