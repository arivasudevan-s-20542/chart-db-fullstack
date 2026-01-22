# Quick Integration Guide

This guide shows how to integrate the Phase 1 features into your existing diagram editor.

## 1. Add Components to Diagram Editor

### Option A: Side Panel Integration
Update your main diagram editor page to include the new panels:

```typescript
// pages/editor/[id].tsx or similar
import { useState } from 'react';
import { DatabasePanel } from '@/components/database';
import { AIAssistantPanel } from '@/components/ai';
import { useTableHighlight } from '@/components/canvas';

export default function DiagramEditor() {
    const [activeTab, setActiveTab] = useState<'database' | 'ai'>('database');
    const { highlightTable } = useTableHighlight();
    const diagramId = 'your-diagram-id'; // Get from router params

    return (
        <div className="h-screen flex">
            {/* Main Canvas Area */}
            <div className="flex-1">
                {/* Your existing ReactFlow canvas */}
            </div>

            {/* Right Sidebar - 400px wide */}
            <div className="w-[400px] border-l bg-white dark:bg-gray-900">
                <div className="flex border-b">
                    <button
                        onClick={() => setActiveTab('database')}
                        className={`flex-1 px-4 py-2 ${
                            activeTab === 'database'
                                ? 'border-b-2 border-blue-500'
                                : 'text-gray-500'
                        }`}
                    >
                        Database
                    </button>
                    <button
                        onClick={() => setActiveTab('ai')}
                        className={`flex-1 px-4 py-2 ${
                            activeTab === 'ai'
                                ? 'border-b-2 border-purple-500'
                                : 'text-gray-500'
                        }`}
                    >
                        AI Assistant
                    </button>
                </div>

                <div className="h-[calc(100vh-48px)]">
                    {activeTab === 'database' && (
                        <DatabasePanel diagramId={diagramId} />
                    )}
                    {activeTab === 'ai' && (
                        <AIAssistantPanel
                            diagramId={diagramId}
                            onHighlightTable={highlightTable}
                        />
                    )}
                </div>
            </div>
        </div>
    );
}
```

### Option B: Modal/Dialog Integration
If you prefer modals instead of a permanent sidebar:

```typescript
import { useState } from 'react';
import { Button } from '@/components/button/button';
import { Dialog, DialogContent } from '@/components/dialog/dialog';
import { DatabasePanel } from '@/components/database';
import { AIAssistantPanel } from '@/components/ai';
import { Database, Bot } from 'lucide-react';

export default function DiagramEditor() {
    const [isDatabaseOpen, setIsDatabaseOpen] = useState(false);
    const [isAIOpen, setIsAIOpen] = useState(false);
    const diagramId = 'your-diagram-id';

    return (
        <div className="h-screen relative">
            {/* Canvas */}
            <div className="h-full">
                {/* Your ReactFlow canvas */}
            </div>

            {/* Floating Action Buttons */}
            <div className="absolute bottom-4 right-4 flex gap-2">
                <Button onClick={() => setIsDatabaseOpen(true)}>
                    <Database className="mr-2 h-4 w-4" />
                    Database
                </Button>
                <Button onClick={() => setIsAIOpen(true)} variant="secondary">
                    <Bot className="mr-2 h-4 w-4" />
                    AI Assistant
                </Button>
            </div>

            {/* Database Modal */}
            <Dialog open={isDatabaseOpen} onOpenChange={setIsDatabaseOpen}>
                <DialogContent className="max-w-[800px] h-[80vh]">
                    <DatabasePanel diagramId={diagramId} />
                </DialogContent>
            </Dialog>

            {/* AI Modal */}
            <Dialog open={isAIOpen} onOpenChange={setIsAIOpen}>
                <DialogContent className="max-w-[600px] h-[80vh]">
                    <AIAssistantPanel diagramId={diagramId} />
                </DialogContent>
            </Dialog>
        </div>
    );
}
```

## 2. Add Table Status Badges to Canvas Nodes

Update your table node component to show status:

```typescript
// components/canvas/TableNode.tsx
import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import { TableStatusBadge, TableStatus } from '@/components/canvas';

interface TableNodeProps {
    data: {
        name: string;
        columns: Array<{ name: string; type: string }>;
        status?: TableStatus; // Add this field
    };
}

export const TableNode = memo(({ data }: TableNodeProps) => {
    return (
        <div className="bg-white border-2 border-gray-300 rounded-lg shadow-lg">
            {/* Header */}
            <div className="px-4 py-2 bg-gray-100 border-b flex items-center justify-between">
                <h3 className="font-semibold">{data.name}</h3>
                {data.status && <TableStatusBadge status={data.status} />}
            </div>

            {/* Columns */}
            <div className="px-4 py-2">
                {data.columns.map((col, idx) => (
                    <div key={idx} className="text-sm py-1">
                        <span className="font-medium">{col.name}</span>
                        <span className="text-gray-500 ml-2">{col.type}</span>
                    </div>
                ))}
            </div>

            <Handle type="target" position={Position.Top} />
            <Handle type="source" position={Position.Bottom} />
        </div>
    );
});
```

## 3. Add Highlighting Support

Integrate the highlighting hooks into your ReactFlow setup:

```typescript
// pages/editor/[id].tsx
import { useCallback, useEffect, useState } from 'react';
import ReactFlow, { useReactFlow } from '@xyflow/react';
import { useTableHighlight, useCanvasPanZoom } from '@/components/canvas';

export default function DiagramEditor() {
    const reactFlowInstance = useReactFlow();
    const { highlightedTables, highlightTable, clearHighlights, isHighlighted } = useTableHighlight();
    const { panToTable } = useCanvasPanZoom(reactFlowInstance);

    // Handle AI suggestions
    const handleHighlightFromAI = useCallback((tableId: string) => {
        highlightTable(tableId);
        panToTable(tableId, 500); // Pan to table with animation
    }, [highlightTable, panToTable]);

    // Clear highlights after 3 seconds
    useEffect(() => {
        if (highlightedTables.size > 0) {
            const timer = setTimeout(clearHighlights, 3000);
            return () => clearTimeout(timer);
        }
    }, [highlightedTables, clearHighlights]);

    // Apply highlight effect to nodes
    const nodesWithHighlight = nodes.map(node => ({
        ...node,
        className: isHighlighted(node.id) 
            ? 'highlighted-node' 
            : '',
    }));

    return (
        <div className="h-screen flex">
            <div className="flex-1">
                <ReactFlow nodes={nodesWithHighlight} edges={edges} />
            </div>
            
            <div className="w-[400px]">
                <AIAssistantPanel
                    diagramId={diagramId}
                    onHighlightTable={handleHighlightFromAI}
                />
            </div>
        </div>
    );
}
```

Add CSS for highlighting effect:

```css
/* globals.css */
.highlighted-node {
    animation: highlight-pulse 2s ease-in-out;
}

@keyframes highlight-pulse {
    0%, 100% {
        box-shadow: 0 0 0 0 rgba(139, 92, 246, 0);
    }
    50% {
        box-shadow: 0 0 0 8px rgba(139, 92, 246, 0.3);
    }
}
```

## 4. Environment Configuration

Add required environment variables:

```env
# .env or .env.local

# Backend API URL
VITE_API_URL=http://localhost:8080

# Optional: If you want to provide default AI keys
VITE_OPENAI_API_KEY=sk-...
VITE_ANTHROPIC_API_KEY=sk-ant-...
```

Backend configuration (already exists):

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chartdb
    username: postgres
    password: password
  
encryption:
  secret-key: your-32-character-secret-key-here
```

## 5. Install Missing Dependencies (if any)

Frontend:
```bash
cd frontend
npm install date-fns framer-motion
```

Backend:
```bash
# All dependencies already in pom.xml
cd backend
mvn clean install
```

## 6. Run the Application

Terminal 1 - Backend:
```bash
cd backend
mvn spring-boot:run
```

Terminal 2 - Frontend:
```bash
cd frontend
npm run dev
```

Visit: http://localhost:5173

## 7. Test the Integration

### Database Connection Test:
1. Click "Database" tab in sidebar
2. Click "Add Connection"
3. Fill in connection details (use your local PostgreSQL)
4. Click "Test Connection" → Should show success
5. Click "Save Connection"
6. Switch to "Query Editor" tab
7. Write SQL: `SELECT * FROM pg_tables LIMIT 5;`
8. Press Cmd/Ctrl + Enter
9. See results in table below

### AI Assistant Test:
1. Click "AI Assistant" tab
2. Click "New Chat"
3. Select "Schema Designer" agent
4. Click "Start Chat"
5. Type: "Add a users table with id, name, email"
6. Press Enter
7. Wait for AI response (will show placeholder for now)
8. Once OpenAI is integrated, you'll see suggested changes
9. Click on a change → table highlights on canvas

### Table Status Test:
1. Add status badges to your table nodes (see step 2)
2. Create a table in diagram → status = PLANNED (gray)
3. Connect to database with that table → status = EXISTS (yellow)
4. Keep them in sync → status = SYNCED (green)
5. Modify diagram → status = DRIFT (red)

## 8. Next Integration Steps

### Add OpenAI Integration:
```typescript
// backend/src/main/java/com/chartdb/service/AIAssistantService.java
// Replace the TODO section with:

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;

private String callOpenAI(String prompt, UserAIConfig config) {
    OpenAIClient client = new OpenAIClientBuilder()
        .credential(new KeyCredential(config.getOpenaiApiKey()))
        .buildClient();
    
    ChatCompletionsOptions options = new ChatCompletionsOptions()
        .setMessages(List.of(
            new ChatMessage(ChatRole.SYSTEM, agentConfig.getSystemPrompt()),
            new ChatMessage(ChatRole.USER, prompt)
        ))
        .setModel("gpt-4");
    
    ChatCompletions response = client.getChatCompletions(
        "gpt-4", 
        options
    );
    
    return response.getChoices().get(0).getMessage().getContent();
}
```

### Add Schema Sync:
```java
// backend/src/main/java/com/chartdb/service/DatabaseConnectionService.java

public void syncSchemaFromDatabase(String connectionId) {
    DatabaseConnection conn = findById(connectionId);
    
    try (Connection jdbcConn = getConnection(conn)) {
        DatabaseMetaData metaData = jdbcConn.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            // Compare with diagram
            // Update status
        }
    }
}
```

## 9. Common Issues & Solutions

### Issue: CORS errors
**Solution:** Check backend CORS config allows frontend origin:
```java
@CrossOrigin(origins = "http://localhost:5173")
```

### Issue: Encryption errors
**Solution:** Set encryption key in application.yml (32 characters)

### Issue: Database connection fails
**Solution:** Check PostgreSQL is running and credentials are correct

### Issue: Monaco editor not loading
**Solution:** Ensure @monaco-editor/react is installed: `npm install @monaco-editor/react`

### Issue: Zustand state not persisting
**Solution:** Add persist middleware if needed:
```typescript
import { persist } from 'zustand/middleware';
```

---

## Complete! 🎉

You now have:
- ✅ Live database connections
- ✅ SQL query execution
- ✅ AI assistant chat
- ✅ Table status tracking
- ✅ Canvas highlighting

**Next:** Integrate OpenAI, test end-to-end, and deploy!
