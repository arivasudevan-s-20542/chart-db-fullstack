# Phase 1 Priority Features - Implementation Plan

> **Focus**: AI Assistant, Database Connection, and Table Status  
> **Updated**: January 8, 2026  
> **Timeline**: 8-10 weeks

---

## 🎯 Top 3 Priority Features

### Priority Order
1. 🤖 **AI Assistant** (VSCode Copilot-style agentic editor)
2. 🔗 **Live Database Connection & Querying**
3. 🏷️ **Mark Table as Existing** (indicates table exists in actual database)

---

## 1. 🤖 AI Assistant - Agentic Editor

### Vision
**VSCode Copilot Edits-style experience** for database schema design. Agent suggests changes, user reviews, and changes are applied with visual feedback.

### Core Architecture

```
User Input → AI Agent → Schema Changes → Visual Highlighting → User Review → Apply/Reject
```

### Key Features

#### 1.1 Agentic Editing Interface
- [ ] **Split-pane editor view** (like VSCode Copilot Edits)
  - Left: Current diagram
  - Right: AI-suggested changes with diff view
  - Bottom: Chat interface

- [ ] **Visual Change Highlighting**
  - 🟢 Green highlight: New tables/columns to be added
  - 🟡 Yellow highlight: Modified elements
  - 🔴 Red highlight: Elements to be removed
  - Real-time preview of changes before applying

- [ ] **Linked Element Focus**
  - When AI suggests changes, automatically:
    - Pan canvas to affected table/area
    - Highlight related tables (foreign key relationships)
    - Show impact radius (what else is affected)
    - Dim unaffected elements

#### 1.2 Multi-Agent System (Configurable)

```typescript
// Agent configuration
interface AgentConfig {
  id: string;
  name: string;
  provider: 'openai' | 'anthropic' | 'custom' | 'mcp';
  model: string;
  capabilities: AgentCapability[];
  systemPrompt?: string;
  temperature?: number;
  maxTokens?: number;
}

type AgentCapability = 
  | 'schema-design'      // Create/modify tables
  | 'query-generation'   // Generate SQL queries
  | 'documentation'      // Write descriptions
  | 'optimization'       // Suggest indexes, normalization
  | 'validation'         // Check for issues
  | 'migration'          // Generate migration scripts
;
```

**Built-in Agents:**
- 🏗️ **Schema Architect** - Design tables and relationships
- 📝 **Documentation Writer** - Auto-generate descriptions
- ⚡ **Performance Optimizer** - Suggest indexes and optimizations
- 🔍 **Schema Validator** - Detect issues and anti-patterns
- 🔄 **Migration Engineer** - Generate migration scripts
- 💬 **Query Assistant** - Write SQL queries

**Custom Agents:**
- Users can create their own agents with custom prompts
- Agent marketplace (future)

#### 1.3 Bring Your Own API Key (BYOK)

```typescript
// User API key management
interface UserAIConfig {
  userId: string;
  providers: {
    openai?: {
      apiKey: string;        // Encrypted at rest
      organization?: string;
      defaultModel: string;  // gpt-4, gpt-4-turbo, etc.
    };
    anthropic?: {
      apiKey: string;
      defaultModel: string;  // claude-3-opus, claude-3-sonnet
    };
    custom?: {
      name: string;
      endpoint: string;
      apiKey: string;
      headers?: Record<string, string>;
    };
  };
  defaultProvider: string;
  fallbackToSystem: boolean;  // Use system API if user's fails
}
```

**Features:**
- ✅ Users provide their own OpenAI/Anthropic/etc. API keys
- ✅ System provides free tier with limited requests
- ✅ API key encryption (AES-256)
- ✅ Usage tracking per user
- ✅ Fallback to system API if user key fails
- ✅ Cost estimation before executing

#### 1.4 Model Context Protocol (MCP) Support

```typescript
// MCP Server Integration
interface MCPServerConfig {
  name: string;
  endpoint: string;
  capabilities: string[];
  authentication?: {
    type: 'bearer' | 'api-key' | 'oauth2';
    credentials: string;
  };
}

// Expose ChartDB as MCP Server
interface ChartDBMCPServer {
  // Tools exposed
  tools: {
    'chartdb/get-schema': (diagramId: string) => Schema;
    'chartdb/create-table': (params: CreateTableParams) => Table;
    'chartdb/modify-table': (params: ModifyTableParams) => Table;
    'chartdb/add-relationship': (params: RelationshipParams) => Relationship;
    'chartdb/query-database': (sql: string) => QueryResult;
    'chartdb/generate-migration': (diff: SchemaDiff) => string;
  };
  
  // Resources exposed
  resources: {
    'chartdb://diagram/:id/schema': SchemaResource;
    'chartdb://diagram/:id/tables': TableListResource;
    'chartdb://diagram/:id/relationships': RelationshipListResource;
  };
  
  // Prompts exposed
  prompts: {
    'analyze-schema': (diagramId: string) => string;
    'suggest-improvements': (diagramId: string) => string;
  };
}
```

**MCP Features:**
- ✅ Expose ChartDB operations as MCP tools
- ✅ Allow external MCP servers to control ChartDB
- ✅ Standard protocol for AI agent integration
- ✅ Support for Claude Desktop, Continue, etc.

#### 1.5 Review & Apply Flow

```
1. User: "Add a tags table with many-to-many relationship to posts"

2. AI Agent analyzes and generates:
   {
     changes: [
       { type: 'CREATE_TABLE', table: 'tags', columns: [...] },
       { type: 'CREATE_TABLE', table: 'post_tags', columns: [...] },
       { type: 'ADD_RELATIONSHIP', from: 'post_tags', to: 'posts' },
       { type: 'ADD_RELATIONSHIP', from: 'post_tags', to: 'tags' }
     ],
     reasoning: "Creating a junction table for many-to-many...",
     affected_elements: ['posts', 'tags', 'post_tags']
   }

3. UI shows split view:
   - Left: Current diagram (posts table highlighted)
   - Right: Preview with 3 new elements (green highlight)
   - Diff panel: Shows exact changes
   
4. User reviews:
   - [Accept All] [Reject All] [Accept Partially]
   - Can edit AI suggestions before applying
   - Can ask follow-up questions
   
5. Apply changes:
   - Animate new elements into place
   - Update canvas
   - Add to changelog
   - Create version snapshot (if enabled)
```

### Database Schema Changes

```sql
-- AI chat sessions and messages
CREATE TABLE ai_chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_config JSONB NOT NULL,  -- AgentConfig
    context JSONB,                 -- Diagram state snapshot
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE ai_messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,  -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    metadata JSONB,              -- Token count, model used, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_session_messages (session_id, created_at)
);

-- AI-suggested changes (for review)
CREATE TABLE ai_suggested_changes (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    message_id VARCHAR(36) REFERENCES ai_messages(id),
    changes JSONB NOT NULL,      -- Array of change operations
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, ACCEPTED, REJECTED, PARTIAL
    applied_changes JSONB,       -- What was actually applied
    applied_at TIMESTAMP,
    applied_by VARCHAR(36) REFERENCES users(id),
    
    INDEX idx_pending_changes (session_id, status)
);

-- User API configurations
CREATE TABLE user_ai_configs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    config JSONB NOT NULL,       -- Encrypted API keys and settings
    usage_stats JSONB,           -- Token usage tracking
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agent definitions (built-in + custom)
CREATE TABLE ai_agents (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),  -- NULL for system agents
    name VARCHAR(100) NOT NULL,
    description TEXT,
    config JSONB NOT NULL,       -- AgentConfig
    is_public BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- MCP server configurations
CREATE TABLE mcp_servers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    config JSONB NOT NULL,       -- MCPServerConfig
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints

```typescript
// Chat & Agent Management
POST   /api/ai/chat/sessions                    // Start new chat session
GET    /api/ai/chat/sessions/:diagramId         // Get sessions for diagram
POST   /api/ai/chat/sessions/:sessionId/message // Send message
GET    /api/ai/chat/sessions/:sessionId/history // Get chat history
DELETE /api/ai/chat/sessions/:sessionId         // End session

// Suggested Changes
GET    /api/ai/changes/:sessionId               // Get pending changes
POST   /api/ai/changes/:changeId/apply          // Apply changes
POST   /api/ai/changes/:changeId/reject         // Reject changes
PATCH  /api/ai/changes/:changeId                // Modify before applying

// Agent Management
GET    /api/ai/agents                           // List available agents
POST   /api/ai/agents                           // Create custom agent
PUT    /api/ai/agents/:agentId                  // Update agent
DELETE /api/ai/agents/:agentId                  // Delete agent

// User API Config
GET    /api/ai/config                           // Get user's AI config
PUT    /api/ai/config                           // Update API keys
POST   /api/ai/config/test                      // Test API key
GET    /api/ai/config/usage                     // Get usage stats

// MCP Integration
GET    /api/mcp/servers                         // List MCP servers
POST   /api/mcp/servers                         // Add MCP server
POST   /api/mcp/servers/:id/test                // Test connection
GET    /api/mcp/.well-known/mcp.json           // MCP server discovery
POST   /api/mcp/tools/:toolName                 // Execute MCP tool
```

### Frontend Components

```typescript
// New components needed
src/
├── components/
│   ├── ai-assistant/
│   │   ├── ai-chat-panel.tsx              // Main chat interface
│   │   ├── ai-diff-viewer.tsx             // Split-pane diff view
│   │   ├── ai-change-preview.tsx          // Visual change preview
│   │   ├── ai-agent-selector.tsx          // Choose agent
│   │   ├── ai-settings-modal.tsx          // Configure API keys
│   │   └── suggested-change-card.tsx      // Review individual change
│   └── canvas/
│       ├── element-highlighter.tsx         // Highlight affected elements
│       └── change-animation.tsx            // Animate new elements
├── context/
│   └── ai-assistant-context/
│       ├── ai-assistant-context.tsx
│       └── ai-assistant-provider.tsx
└── services/
    └── api/
        ├── ai.api.ts                       // AI API client
        └── mcp.api.ts                      // MCP client
```

### User Experience Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Editor Layout                                               │
│  ┌────────────────────┬────────────────────┐                │
│  │  Canvas            │  AI Diff Preview   │                │
│  │  (Current)         │  (Suggested)       │                │
│  │                    │                    │                │
│  │  [Table: users]    │  [Table: users]    │  🟡 Modified  │
│  │  - id              │  - id              │                │
│  │  - name            │  - name            │                │
│  │                    │  - email ✨        │  🟢 New       │
│  │                    │                    │                │
│  │                    │  [Table: tags] ✨  │  🟢 New       │
│  │                    │  - id              │                │
│  │                    │  - name            │                │
│  └────────────────────┴────────────────────┘                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  AI Assistant Chat                                      │ │
│  │  User: Add email field to users and create tags table  │ │
│  │  AI: I'll add an email column to users table and       │ │
│  │      create a new tags table. Review changes above. →  │ │
│  │  [Accept All] [Reject] [Modify] [Ask Follow-up]        │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### System Architecture

```
Frontend (React)
    ↓ (WebSocket + REST)
Backend (Spring Boot)
    ↓
┌───────────────────────────────────────┐
│  AI Orchestration Service             │
│  ├── Agent Manager                    │
│  ├── Context Builder (diagram state)  │
│  ├── Change Generator                 │
│  └── Change Applier                   │
└───────────────────────────────────────┘
    ↓
┌───────────────────────────────────────┐
│  LLM Provider (configurable)          │
│  ├── OpenAI (user key or system)     │
│  ├── Anthropic (user key or system)  │
│  ├── Custom endpoint                  │
│  └── MCP Server                       │
└───────────────────────────────────────┘
```

---

## 2. 🔗 Live Database Connection & Querying

### Features

#### 2.1 Connection Management
- [ ] **Save encrypted database connections**
  - Support: PostgreSQL, MySQL, SQL Server, Oracle, MongoDB
  - Store encrypted credentials per diagram
  - Test connection before saving
  - Connection status indicator (🟢 online / 🔴 offline)

- [ ] **Multiple connection profiles**
  - Dev, Staging, Production
  - Quick switch between environments
  - Connection labeling and color coding

#### 2.2 Live Querying
- [ ] **SQL Query Editor** (Monaco Editor integration - already installed)
  - Syntax highlighting per database type
  - Auto-completion (tables, columns from diagram)
  - Query history
  - Saved queries
  - Query templates

- [ ] **Query Execution**
  - Run queries against connected database
  - Show results in table view
  - Export results (CSV, JSON, Excel)
  - Query timeout settings
  - Transaction support (BEGIN, COMMIT, ROLLBACK)

- [ ] **Visual Query Builder**
  - Click tables/columns to build SELECT
  - Visual JOIN builder
  - WHERE clause builder
  - Generate SQL from visual builder

#### 2.3 Schema Synchronization
- [ ] **Import from Database** (Reverse Engineering)
  - Scan connected database
  - Import tables, columns, constraints
  - Import indexes, triggers, views
  - Incremental import (only new tables)
  - Conflict resolution (if table exists in diagram)

- [ ] **Export to Database** (Forward Engineering)
  - Generate CREATE statements
  - Execute against connected database
  - Preview changes before execution
  - Rollback support

- [ ] **Two-Way Sync**
  - Detect schema drift
  - Show diff between diagram and database
  - Sync options: diagram → DB or DB → diagram
  - Auto-sync on schedule (optional)

#### 2.4 Real-Time Validation
- [ ] **Validate against live database**
  - Check if table exists
  - Verify column data types match
  - Validate relationships (FK constraints exist)
  - Show validation status per table

### Database Schema

```sql
-- Database connections
CREATE TABLE database_connections (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,              -- "Production DB", "Dev DB"
    database_type VARCHAR(50) NOT NULL,       -- postgresql, mysql, etc.
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    encrypted_password TEXT NOT NULL,         -- AES-256 encrypted
    ssl_enabled BOOLEAN DEFAULT true,
    additional_params JSONB,                  -- Connection string params
    status VARCHAR(20) DEFAULT 'UNKNOWN',     -- CONNECTED, ERROR, UNKNOWN
    last_connected_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_diagram_connections (diagram_id)
);

-- Query history
CREATE TABLE query_history (
    id VARCHAR(36) PRIMARY KEY,
    connection_id VARCHAR(36) REFERENCES database_connections(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    query TEXT NOT NULL,
    execution_time_ms INTEGER,
    rows_affected INTEGER,
    status VARCHAR(20),                       -- SUCCESS, ERROR
    error_message TEXT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_history (user_id, executed_at DESC)
);

-- Saved queries
CREATE TABLE saved_queries (
    id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    query TEXT NOT NULL,
    tags VARCHAR(500),
    is_public BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Schema sync status
CREATE TABLE schema_sync_status (
    diagram_id VARCHAR(36) PRIMARY KEY REFERENCES diagrams(id) ON DELETE CASCADE,
    connection_id VARCHAR(36) REFERENCES database_connections(id),
    last_sync_at TIMESTAMP,
    sync_direction VARCHAR(20),               -- TO_DB, FROM_DB, TWO_WAY
    tables_synced INTEGER,
    drift_detected BOOLEAN DEFAULT false,
    drift_details JSONB,
    next_auto_sync TIMESTAMP
);
```

### API Endpoints

```typescript
// Connection Management
POST   /api/diagrams/:id/connections              // Add connection
GET    /api/diagrams/:id/connections              // List connections
PUT    /api/diagrams/:id/connections/:connId      // Update connection
DELETE /api/diagrams/:id/connections/:connId      // Delete connection
POST   /api/diagrams/:id/connections/:connId/test // Test connection

// Query Execution
POST   /api/connections/:connId/query             // Execute query
GET    /api/connections/:connId/history           // Query history
POST   /api/connections/:connId/queries           // Save query
GET    /api/connections/:connId/queries           // List saved queries

// Schema Sync
POST   /api/diagrams/:id/sync/import              // Import from DB
POST   /api/diagrams/:id/sync/export              // Export to DB
GET    /api/diagrams/:id/sync/diff                // Show schema diff
POST   /api/diagrams/:id/sync/auto                // Enable auto-sync
GET    /api/diagrams/:id/sync/status              // Sync status

// Validation
POST   /api/diagrams/:id/validate                 // Validate against DB
```

### Frontend Components

```typescript
src/
├── components/
│   ├── database-connection/
│   │   ├── connection-modal.tsx           // Add/Edit connection
│   │   ├── connection-selector.tsx        // Switch connections
│   │   ├── connection-status.tsx          // Status indicator
│   │   └── test-connection-dialog.tsx     // Test UI
│   ├── query-editor/
│   │   ├── sql-editor.tsx                 // Monaco SQL editor
│   │   ├── query-results.tsx              // Results table
│   │   ├── query-history-panel.tsx        // History sidebar
│   │   └── visual-query-builder.tsx       // Visual builder
│   └── schema-sync/
│       ├── sync-modal.tsx                 // Sync wizard
│       ├── schema-diff-viewer.tsx         // Show differences
│       └── sync-conflict-resolver.tsx     // Handle conflicts
└── services/
    └── api/
        └── database.api.ts                 // Database API client
```

---

## 3. 🏷️ Mark Table as Existing

### Purpose
**NOT deprecation** - this marks tables that **already exist in the connected database** vs tables that are only in the diagram (planned/future tables).

### Use Cases
1. **Reverse engineering**: Imported tables marked as "existing"
2. **Mixed design**: Some tables exist in DB, others are planned
3. **Visual distinction**: Easy to see what's real vs what's planned
4. **Sync safety**: Prevent accidental overwrites of existing tables

### Features

- [ ] **Table Status Types**
  ```typescript
  enum TableStatus {
    PLANNED = 'PLANNED',       // Only in diagram (default)
    EXISTS = 'EXISTS',         // Exists in connected database
    SYNCED = 'SYNCED',        // Diagram and DB are in sync
    DRIFT = 'DRIFT',          // Exists but diagram differs from DB
    DEPRECATED = 'DEPRECATED'  // Marked for removal (future)
  }
  ```

- [ ] **Visual Indicators**
  - 🔵 **PLANNED**: Default look (no indicator)
  - 🟢 **EXISTS**: Green badge "Live" + database icon
  - ✅ **SYNCED**: Green checkmark badge
  - 🟡 **DRIFT**: Yellow warning badge "Out of sync"
  - 🔴 **DEPRECATED**: Red strikethrough (future feature)

- [ ] **Automatic Status Updates**
  - When importing from database → mark as EXISTS
  - When syncing → mark as SYNCED
  - When drift detected → mark as DRIFT
  - Manual toggle allowed

- [ ] **Status-Based Actions**
  - EXISTS tables: "Sync from DB", "View in DB"
  - PLANNED tables: "Create in DB", "Export SQL"
  - DRIFT tables: "Show differences", "Resolve"

### Database Schema

```sql
-- Add to existing diagram_tables table
ALTER TABLE diagram_tables 
ADD COLUMN status VARCHAR(20) DEFAULT 'PLANNED',
ADD COLUMN source_database VARCHAR(100),      -- Which DB it came from
ADD COLUMN last_verified_at TIMESTAMP,        -- When status was checked
ADD COLUMN sync_metadata JSONB;               -- Sync details

-- Index for filtering by status
CREATE INDEX idx_table_status ON diagram_tables(status);
```

### API Endpoints

```typescript
// Table Status
PATCH  /api/diagrams/:id/tables/:tableId/status   // Update status
POST   /api/diagrams/:id/tables/:tableId/verify   // Verify against DB
GET    /api/diagrams/:id/tables/status-summary    // Count by status
```

### UI/UX

```
┌────────────────────────────────┐
│  users                     🟢  │  ← Green "Live" badge
│  ────────────────────────────  │
│  🔑 id: bigint                 │
│  📧 email: varchar(255)        │
│  📅 created_at: timestamp      │
│  ↗️  → posts (1:N)             │
└────────────────────────────────┘

┌────────────────────────────────┐
│  analytics                  🔵 │  ← No badge (planned)
│  ────────────────────────────  │
│  🔑 id: bigint                 │
│  📊 event_name: varchar        │
│  📅 tracked_at: timestamp      │
└────────────────────────────────┘

┌────────────────────────────────┐
│  orders                     🟡 │  ← Yellow "Drift" badge
│  ────────────────────────────  │
│  🔑 id: bigint                 │
│  💰 total: decimal             │  ← Different in DB
│  ⚠️  [View Differences]        │
└────────────────────────────────┘
```

---

## 📊 Implementation Timeline

### Week 1-2: Database Connection Foundation
- [ ] Database connection CRUD
- [ ] Encrypted credential storage
- [ ] Connection testing
- [ ] Basic query execution

### Week 3-4: Schema Sync & Table Status
- [ ] Reverse engineering (import from DB)
- [ ] Table status system (PLANNED/EXISTS/SYNCED/DRIFT)
- [ ] Visual indicators on canvas
- [ ] Schema diff detection

### Week 5-6: AI Assistant Core
- [ ] AI chat UI (split-pane layout)
- [ ] Basic agent system
- [ ] Context injection (diagram state)
- [ ] Change preview UI

### Week 7-8: AI Agentic Features
- [ ] Multi-agent support
- [ ] BYOK (API key management)
- [ ] Visual change highlighting
- [ ] Review & apply flow

### Week 9-10: MCP & Polish
- [ ] MCP server implementation
- [ ] Agent marketplace (basic)
- [ ] Query editor polish
- [ ] Testing & bug fixes

---

## 🛠️ Technical Stack Additions

### Backend
```
New Dependencies:
├── spring-ai (optional - for AI orchestration)
├── HikariCP (already have - connection pooling)
├── Jasypt (encryption for API keys & DB passwords)
└── WebSocket (already have - for real-time AI streaming)
```

### Frontend
```
Already Have:
├── @ai-sdk/openai ✅
├── ai ✅
├── @monaco-editor/react ✅

Need to Add:
├── @anthropic-ai/sdk (for Claude)
├── react-diff-viewer (for change preview)
└── react-split-pane (for layout)
```

---

## 🎯 Success Metrics

### AI Assistant
- [ ] 80%+ user satisfaction with AI suggestions
- [ ] Average 3-5 interactions per session
- [ ] 70%+ acceptance rate for AI changes
- [ ] <3s response time for AI

### Database Connection
- [ ] 100+ databases connected in first month
- [ ] 50%+ users use query editor
- [ ] 90%+ successful reverse engineering

### Table Status
- [ ] 100% accuracy in status detection
- [ ] <1s status verification time
- [ ] Used in 80%+ diagrams with DB connections

---

## 🔒 Security Considerations

1. **API Key Storage**: AES-256 encryption, never logged
2. **Database Credentials**: Encrypted at rest, decrypted only for connections
3. **SQL Injection**: Parameterized queries, input sanitization
4. **Rate Limiting**: Prevent API abuse (100 req/min per user)
5. **MCP Security**: Token-based auth, scope restrictions

---

## 💰 Cost Considerations

### AI Token Usage
- **System API**: $500/month budget (free tier users)
- **User BYOK**: No cost to us
- **Caching**: Cache diagram context, reduce tokens by 60%

### Database Connections
- Connection pooling to reduce overhead
- Timeout limits to prevent hanging connections

---

_Next: Start with Database Connection or AI Assistant core?_
