# 03 - Database Schema

## 🗄️ Complete PostgreSQL Schema

### Overview

| Table | Purpose |
|-------|---------|
| `users` | User accounts and authentication |
| `diagrams` | Main diagram/canvas records |
| `tables` | Database tables/entities in diagrams |
| `columns` | Columns/fields within tables |
| `relationships` | Connections between tables |
| `diagram_permissions` | Access control for diagrams |
| `active_collaborators` | Real-time presence tracking |
| `diagram_versions` | Version history |
| `audit_logs` | Action audit trail |
| `table_locks` | Optimistic locking for concurrent edits |

---

## 📋 SQL Schema Definitions

### 1. Users Table
```sql
-- Users table for authentication and user profiles
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),
    cursor_color VARCHAR(7) DEFAULT '#3B82F6',  -- Hex color for collaboration cursor
    
    -- Settings
    preferences JSONB DEFAULT '{}',
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 2. Diagrams Table
```sql
-- Diagrams table - main canvas/project
CREATE TABLE diagrams (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
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
    database_type VARCHAR(50) DEFAULT 'postgresql',  -- postgresql, mysql, sqlite, etc.
    schema_name VARCHAR(100) DEFAULT 'public',
    
    -- Metadata
    tags VARCHAR(255)[],
    category VARCHAR(100),
    
    -- Version Control
    current_version_id VARCHAR(36),
    version_number INTEGER DEFAULT 1,
    
    -- Status
    status VARCHAR(20) DEFAULT 'active',  -- active, archived, deleted
    is_template BOOLEAN DEFAULT false,
    
    -- Statistics
    view_count INTEGER DEFAULT 0,
    export_count INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    
    -- Indexes
    CONSTRAINT valid_zoom CHECK (canvas_zoom >= 0.1 AND canvas_zoom <= 10.0),
    CONSTRAINT valid_status CHECK (status IN ('active', 'archived', 'deleted'))
);

CREATE INDEX idx_diagrams_owner ON diagrams(owner_id);
CREATE INDEX idx_diagrams_public ON diagrams(is_public) WHERE is_public = true;
CREATE INDEX idx_diagrams_slug ON diagrams(public_slug) WHERE public_slug IS NOT NULL;
CREATE INDEX idx_diagrams_status ON diagrams(status);
CREATE INDEX idx_diagrams_tags ON diagrams USING GIN(tags);

CREATE TRIGGER update_diagrams_updated_at
    BEFORE UPDATE ON diagrams
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 3. Tables Table (Entities)
```sql
-- Tables (database entities) within diagrams
-- CRITICAL: position_x and position_y must always be saved!
CREATE TABLE tables (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
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
    height DECIMAL(10,2),  -- Auto-calculated based on columns
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
    table_type VARCHAR(50) DEFAULT 'table',  -- table, view, enum, interface
    schema_name VARCHAR(100) DEFAULT 'public',
    
    -- Ordering
    z_index INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    
    -- Comments/Notes
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
```

### 4. Columns Table (Fields)
```sql
-- Columns (fields) within tables
CREATE TABLE columns (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    
    -- Data Type
    data_type VARCHAR(100) NOT NULL,
    native_type VARCHAR(100),  -- Database-specific type
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
    default_expression TEXT,  -- For computed defaults
    
    -- Foreign Key Info (if is_foreign_key = true)
    fk_table_id VARCHAR(36) REFERENCES tables(id) ON DELETE SET NULL,
    fk_column_id VARCHAR(36),  -- Self-reference, added after columns created
    fk_constraint_name VARCHAR(255),
    fk_on_delete VARCHAR(20) DEFAULT 'NO ACTION',
    fk_on_update VARCHAR(20) DEFAULT 'NO ACTION',
    
    -- Check Constraints
    check_constraint TEXT,
    
    -- Enum Values (if data_type is enum)
    enum_values TEXT[],
    
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
    CONSTRAINT unique_column_name_per_table UNIQUE (table_id, name),
    CONSTRAINT unique_column_order_per_table UNIQUE (table_id, order_index)
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
```

### 5. Relationships Table
```sql
-- Relationships between tables
CREATE TABLE relationships (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    
    -- Source and Target
    source_table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    target_table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    source_column_id VARCHAR(36) REFERENCES columns(id) ON DELETE SET NULL,
    target_column_id VARCHAR(36) REFERENCES columns(id) ON DELETE SET NULL,
    
    -- Relationship Type
    relationship_type VARCHAR(50) NOT NULL,  -- ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
    cardinality_source VARCHAR(10) DEFAULT '1',   -- '1', '0..1', '1..*', '0..*'
    cardinality_target VARCHAR(10) DEFAULT '*',
    
    -- Naming
    name VARCHAR(255),
    constraint_name VARCHAR(255),  -- FK constraint name
    
    -- Visual/Path (for custom line routing)
    path_type VARCHAR(20) DEFAULT 'auto',  -- auto, straight, orthogonal, curved
    path_points JSONB,  -- Array of {x, y} points for custom paths
    source_handle VARCHAR(20) DEFAULT 'right',  -- top, right, bottom, left
    target_handle VARCHAR(20) DEFAULT 'left',
    
    -- Styling
    line_color VARCHAR(7) DEFAULT '#94A3B8',
    line_width DECIMAL(3,1) DEFAULT 2.0,
    line_style VARCHAR(20) DEFAULT 'solid',  -- solid, dashed, dotted
    
    -- Labels
    label_source VARCHAR(100),
    label_target VARCHAR(100),
    show_labels BOOLEAN DEFAULT true,
    
    -- Referential Actions
    on_delete VARCHAR(20) DEFAULT 'NO ACTION',
    on_update VARCHAR(20) DEFAULT 'NO ACTION',
    
    -- Flags
    is_identifying BOOLEAN DEFAULT false,  -- Strong vs weak relationship
    is_virtual BOOLEAN DEFAULT false,  -- Logical only, not in DB
    
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
```

### 6. Diagram Permissions
```sql
-- Access control for diagrams
CREATE TABLE diagram_permissions (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    
    -- For team/email invitations
    invited_email VARCHAR(255),
    
    -- Permission Level
    permission_level VARCHAR(20) NOT NULL DEFAULT 'viewer',  -- owner, editor, commenter, viewer
    
    -- Granular Permissions
    can_view BOOLEAN DEFAULT true,
    can_edit BOOLEAN DEFAULT false,
    can_comment BOOLEAN DEFAULT false,
    can_share BOOLEAN DEFAULT false,
    can_export BOOLEAN DEFAULT true,
    can_delete BOOLEAN DEFAULT false,
    can_manage_permissions BOOLEAN DEFAULT false,
    
    -- Invitation Status
    invitation_status VARCHAR(20) DEFAULT 'accepted',  -- pending, accepted, declined
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
        permission_level IN ('owner', 'editor', 'commenter', 'viewer')
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
```

### 7. Active Collaborators (Real-time Presence)
```sql
-- Real-time presence tracking
CREATE TABLE active_collaborators (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
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
    status VARCHAR(20) DEFAULT 'active',  -- active, idle, away
    
    -- Device Info
    device_type VARCHAR(20),  -- desktop, tablet, mobile
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

-- Auto-cleanup old sessions (for use with scheduled job)
CREATE INDEX idx_collaborators_stale ON active_collaborators(last_seen) WHERE is_active = true;
```

### 8. Diagram Versions
```sql
-- Version history for diagrams
CREATE TABLE diagram_versions (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
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
```

### 9. Audit Logs
```sql
-- Audit trail for all actions
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    
    -- Context
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    diagram_id VARCHAR(36) REFERENCES diagrams(id) ON DELETE CASCADE,
    session_id VARCHAR(100),
    
    -- Action
    action_type VARCHAR(50) NOT NULL,  -- CREATE, UPDATE, DELETE, VIEW, EXPORT, SHARE
    entity_type VARCHAR(50) NOT NULL,  -- diagram, table, column, relationship
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
    
    -- Indexes for querying
    CONSTRAINT valid_action_type CHECK (
        action_type IN ('CREATE', 'UPDATE', 'DELETE', 'VIEW', 'EXPORT', 'SHARE', 'LOGIN', 'LOGOUT')
    )
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_diagram ON audit_logs(diagram_id);
CREATE INDEX idx_audit_action ON audit_logs(action_type);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_logs(created_at);

-- Partitioning for large audit logs (optional)
-- Consider partitioning by month for production
```

### 10. Table Locks (Optimistic Locking)
```sql
-- Optimistic locking for concurrent edits
CREATE TABLE table_locks (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    table_id VARCHAR(36) NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Lock Info
    lock_type VARCHAR(20) DEFAULT 'edit',  -- edit, move, delete
    
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
```

---

## 🔄 Migration Scripts (Flyway)

### V1__Initial_Schema.sql
```sql
-- Include all CREATE TABLE statements above
-- Run in order: users, diagrams, tables, columns, relationships, 
-- diagram_permissions, active_collaborators, diagram_versions, audit_logs, table_locks
```

### V2__Seed_Data.sql
```sql
-- Optional: Insert default/demo data
INSERT INTO users (id, email, password_hash, username, display_name)
VALUES 
    ('system', 'system@chartdb.io', '', 'system', 'System'),
    ('demo', 'demo@chartdb.io', '$2a$10$...', 'demo', 'Demo User');
```

---

## 📊 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DATABASE ENTITY RELATIONSHIPS                            │
└─────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────┐
                                    │   users     │
                                    ├─────────────┤
                                    │ id (PK)     │
                                    │ email       │
                                    │ username    │
                                    └──────┬──────┘
                                           │
              ┌────────────────────────────┼────────────────────────────┐
              │                            │                            │
              ▼                            ▼                            ▼
     ┌────────────────┐          ┌─────────────────┐          ┌────────────────┐
     │   diagrams     │          │diagram_permissions│        │  audit_logs    │
     ├────────────────┤          ├─────────────────┤          ├────────────────┤
     │ id (PK)        │◄─────────│ diagram_id (FK) │          │ user_id (FK)   │
     │ owner_id (FK)  │          │ user_id (FK)    │          │ diagram_id(FK) │
     │ name           │          │ permission_level│          │ action_type    │
     │ canvas_zoom    │          └─────────────────┘          └────────────────┘
     │ canvas_offset_x│
     │ canvas_offset_y│
     └───────┬────────┘
             │
    ┌────────┼────────────────────────┐
    │        │                        │
    ▼        ▼                        ▼
┌─────────┐ ┌──────────────────┐ ┌────────────────────┐
│ tables  │ │  relationships   │ │ active_collaborators│
├─────────┤ ├──────────────────┤ ├────────────────────┤
│id (PK)  │ │ id (PK)          │ │ diagram_id (FK)    │
│diagram_id│◄│ diagram_id (FK)  │ │ user_id (FK)       │
│name     │ │ source_table_id  │ │ cursor_x           │
│position_x│ │ target_table_id  │ │ cursor_y           │
│position_y│ │ relationship_type│ └────────────────────┘
│width    │ └──────────────────┘
│height   │
└────┬────┘
     │
     ▼
┌─────────────┐
│   columns   │
├─────────────┤
│ id (PK)     │
│ table_id(FK)│
│ name        │
│ data_type   │
│ is_primary_key│
│ is_foreign_key│
│ order_index │
└─────────────┘
```

---

## ⚙️ Performance Indexes Summary

```sql
-- Critical indexes for common queries

-- User lookups
CREATE INDEX idx_users_email ON users(email);

-- Diagram queries
CREATE INDEX idx_diagrams_owner ON diagrams(owner_id);
CREATE INDEX idx_diagrams_public ON diagrams(is_public) WHERE is_public = true;

-- Table position queries (CRITICAL for canvas rendering)
CREATE INDEX idx_tables_diagram ON tables(diagram_id);
CREATE INDEX idx_tables_position ON tables(diagram_id, position_x, position_y);

-- Column ordering
CREATE INDEX idx_columns_table ON columns(table_id);
CREATE INDEX idx_columns_order ON columns(table_id, order_index);

-- Relationship lookups
CREATE INDEX idx_relationships_diagram ON relationships(diagram_id);
CREATE INDEX idx_relationships_source ON relationships(source_table_id);
CREATE INDEX idx_relationships_target ON relationships(target_table_id);

-- Permission checks
CREATE INDEX idx_permissions_user ON diagram_permissions(user_id);
CREATE INDEX idx_permissions_diagram ON diagram_permissions(diagram_id);

-- Active collaborators (real-time)
CREATE INDEX idx_collaborators_active ON active_collaborators(diagram_id, is_active) 
    WHERE is_active = true;
```

---

**← Previous:** `02-ARCHITECTURE.md` | **Next:** `04-BACKEND-STRUCTURE.md` →
