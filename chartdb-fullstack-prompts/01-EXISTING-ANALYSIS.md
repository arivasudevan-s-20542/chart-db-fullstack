# 01 - Existing ChartDB Analysis

## 📍 Source Location
```
/home/workspace/PLAYBOOKS_DATASPACE/chartdb
```

## 🔍 Pre-Implementation Analysis Tasks

Before implementing, the LLM should analyze the existing codebase to understand:

### 1. Project Structure Analysis
```bash
# Commands to run for analysis
cd /home/workspace/PLAYBOOKS_DATASPACE/chartdb

# List all directories
find . -type d -name "node_modules" -prune -o -type d -print

# Find all TypeScript/JavaScript files
find . -name "*.ts" -o -name "*.tsx" | grep -v node_modules

# Check package.json for dependencies
cat package.json

# Look for state management
grep -r "zustand\|redux\|recoil\|jotai" --include="*.ts" --include="*.tsx" | head -20

# Find IndexedDB usage
grep -r "indexedDB\|IndexedDB\|idb\|Dexie" --include="*.ts" --include="*.tsx"

# Find store/state files
find . -name "*store*" -o -name "*Store*" | grep -v node_modules

# Find canvas/diagram components
find . -name "*canvas*" -o -name "*Canvas*" -o -name "*diagram*" -o -name "*Diagram*" | grep -v node_modules
```

### 2. Key Files to Examine

| File Pattern | Purpose |
|-------------|---------|
| `**/store/**` or `**/stores/**` | State management |
| `**/hooks/**` | Custom React hooks |
| `**/context/**` | React context providers |
| `**/components/canvas/**` | Canvas rendering |
| `**/components/table/**` | Table/entity components |
| `**/types/**` or `**/*.d.ts` | TypeScript interfaces |
| `**/db/**` or `**/indexeddb/**` | Local storage implementation |
| `**/utils/**` | Utility functions |
| `vite.config.ts` | Build configuration |
| `tsconfig.json` | TypeScript configuration |

### 3. Critical Data Structures to Identify

#### Expected Diagram Interface (approximate)
```typescript
// Find and document the actual interfaces
interface Diagram {
  id: string;
  name: string;
  tables: Table[];
  relationships: Relationship[];
  // Canvas settings
  zoom?: number;
  panX?: number;
  panY?: number;
}

interface Table {
  id: string;
  name: string;
  x: number;        // ← CRITICAL: Position X
  y: number;        // ← CRITICAL: Position Y
  columns: Column[];
  color?: string;
  width?: number;
  height?: number;
}

interface Column {
  id: string;
  name: string;
  type: string;
  isPrimaryKey: boolean;
  isForeignKey: boolean;
  isNullable: boolean;
  isUnique: boolean;
  defaultValue?: string;
}

interface Relationship {
  id: string;
  sourceTableId: string;
  targetTableId: string;
  sourceColumnId?: string;
  targetColumnId?: string;
  type: 'one-to-one' | 'one-to-many' | 'many-to-many';
}
```

### 4. State Management Patterns

#### Look for existing patterns like:
```typescript
// Zustand example (common in modern React apps)
const useDiagramStore = create<DiagramState>((set, get) => ({
  diagram: null,
  tables: [],
  
  addTable: (table) => set((state) => ({
    tables: [...state.tables, table]
  })),
  
  updateTablePosition: (id, x, y) => set((state) => ({
    tables: state.tables.map(t => 
      t.id === id ? { ...t, x, y } : t
    )
  })),
  
  // ... more actions
}));
```

### 5. IndexedDB Implementation

#### Find how data is currently persisted:
```typescript
// Common IndexedDB patterns to look for:

// Using idb library
import { openDB } from 'idb';

const db = await openDB('chartdb', 1, {
  upgrade(db) {
    db.createObjectStore('diagrams', { keyPath: 'id' });
  }
});

// Using Dexie
import Dexie from 'dexie';

class ChartDB extends Dexie {
  diagrams: Dexie.Table<Diagram, string>;
  
  constructor() {
    super('chartdb');
    this.version(1).stores({
      diagrams: 'id, name, createdAt'
    });
  }
}

// Raw IndexedDB
const request = indexedDB.open('chartdb', 1);
```

### 6. Canvas Implementation

#### Identify canvas library and patterns:
```typescript
// Check for canvas libraries:
// - react-konva (Konva.js wrapper)
// - fabric.js
// - react-flow
// - Custom canvas implementation

// Look for drag handlers
onDragStart={(e) => { /* ... */ }}
onDragMove={(e) => { 
  // THIS IS WHERE WE'LL ADD WEBSOCKET SYNC
  updatePosition(tableId, e.target.x(), e.target.y());
}}
onDragEnd={(e) => { /* ... */ }}
```

---

## 📝 Analysis Output Template

After analyzing, document findings in this format:

```markdown
## ChartDB Analysis Results

### 1. Framework & Build
- **Framework:** React [version]
- **Build Tool:** Vite [version]
- **Language:** TypeScript [version]
- **Package Manager:** npm/yarn/pnpm

### 2. State Management
- **Library:** [Zustand/Redux/Context/etc.]
- **Store Location:** [path to store files]
- **State Shape:** [describe structure]

### 3. Data Persistence
- **Method:** [IndexedDB library name]
- **DB Name:** [database name]
- **Tables:** [list of object stores]
- **File Location:** [path to DB implementation]

### 4. Canvas/Diagram
- **Canvas Library:** [library name if any]
- **Main Canvas Component:** [component path]
- **Table Component:** [component path]
- **Drag Handler Location:** [where drag events are handled]

### 5. Key Interfaces
- **Diagram Interface:** [file path]
- **Table Interface:** [file path]
- **Column Interface:** [file path]
- **Relationship Interface:** [file path]

### 6. Entry Points for Integration
- **API Integration Point:** [where to add API calls]
- **WebSocket Integration Point:** [where to add WS logic]
- **Auth Integration Point:** [where to add auth]

### 7. Potential Challenges
- [List any identified challenges]
```

---

## 🎯 Integration Points Summary

After analysis, identify these specific integration points:

### 1. State Store Integration
```typescript
// Location: [path to store]
// We'll add API sync here

// BEFORE (existing)
updateTablePosition: (id, x, y) => {
  set((state) => ({
    tables: state.tables.map(t => t.id === id ? { ...t, x, y } : t)
  }));
  // Saves to IndexedDB
}

// AFTER (with backend integration)
updateTablePosition: (id, x, y) => {
  set((state) => ({
    tables: state.tables.map(t => t.id === id ? { ...t, x, y } : t)
  }));
  
  // NEW: Sync to backend (debounced)
  debouncedApiSync.updateTablePosition(id, x, y);
  
  // NEW: Broadcast via WebSocket
  websocketService.send('/app/table-move', { 
    diagramId, tableId: id, x, y 
  });
}
```

### 2. WebSocket Event Handlers
```typescript
// NEW FILE: src/hooks/useCollaborativeSync.ts

useEffect(() => {
  // Subscribe to diagram updates
  const unsubscribe = websocketService.subscribe(
    `/topic/diagram/${diagramId}/table-moved`,
    (message) => {
      // Update local state with remote changes
      if (message.userId !== currentUserId) {
        store.updateTablePositionFromRemote(
          message.tableId, 
          message.x, 
          message.y
        );
      }
    }
  );
  
  return () => unsubscribe();
}, [diagramId]);
```

### 3. Canvas Component Integration
```typescript
// Location: [path to canvas or table component]

// Add cursor overlay layer
<CollaboratorCursors diagramId={diagramId} />

// Modify drag handlers
onDragMove={(e) => {
  const { x, y } = e.target.position();
  
  // Existing local update
  updateTablePosition(tableId, x, y);
  
  // NEW: Broadcast move (throttled)
  throttledBroadcast('table-move', { tableId, x, y });
}}
```

### 4. App Initialization
```typescript
// Location: [main App.tsx or index.tsx]

// NEW: Add providers
<AuthProvider>
  <WebSocketProvider>
    <CollaborationProvider>
      {/* Existing app content */}
    </CollaborationProvider>
  </WebSocketProvider>
</AuthProvider>
```

---

## 📋 Pre-Implementation Checklist

Before starting implementation, confirm:

- [ ] Located the state management store
- [ ] Identified IndexedDB implementation
- [ ] Found table position update function
- [ ] Located drag event handlers
- [ ] Identified TypeScript interfaces for Diagram/Table/Column
- [ ] Found the main canvas/diagram component
- [ ] Checked for existing API/fetch calls
- [ ] Reviewed package.json dependencies
- [ ] Tested that existing app runs correctly
- [ ] Created backup copy of original codebase

---

## ⚠️ Common Gotchas

### 1. Position Coordinate System
- Canvas libraries may use different coordinate systems
- Some use top-left origin, others center
- Check if positions are absolute or relative to viewport

### 2. State Immutability
- Ensure all state updates are immutable
- Check if existing code uses immer or similar

### 3. Event Debouncing
- Drag events fire rapidly (60+ times/second)
- Must debounce/throttle for network calls

### 4. Canvas Rendering
- Some canvas libraries re-render differently
- May need to optimize remote update rendering

---

**← Previous:** `00-OVERVIEW.md` | **Next:** `02-ARCHITECTURE.md` →
