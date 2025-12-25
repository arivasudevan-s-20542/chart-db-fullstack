# ChartDB Full-Stack Enhancement Project

## 🎯 Project Overview

**Objective:** Transform an existing frontend-only ERD tool (ChartDB) into a full-stack collaborative diagramming platform with real-time multi-user editing capabilities similar to Figma.

---

## 📋 Table of Contents

| File | Description |
|------|-------------|
| `00-OVERVIEW.md` | This file - Project summary and navigation |
| `01-EXISTING-ANALYSIS.md` | Analysis of existing ChartDB frontend |
| `02-ARCHITECTURE.md` | System architecture and design decisions |
| `03-DATABASE-SCHEMA.md` | Complete PostgreSQL database design |
| `04-BACKEND-STRUCTURE.md` | Spring Boot project structure and setup |
| `05-BACKEND-ENTITIES.md` | JPA Entity classes with annotations |
| `06-BACKEND-REPOSITORIES.md` | Spring Data JPA repositories |
| `07-BACKEND-SERVICES.md` | Business logic service layer |
| `08-BACKEND-CONTROLLERS.md` | REST API controllers |
| `09-WEBSOCKET-CONFIG.md` | WebSocket + STOMP configuration |
| `10-WEBSOCKET-HANDLERS.md` | Real-time event handlers |
| `11-SECURITY-AUTH.md` | Spring Security + JWT implementation |
| `12-FRONTEND-INTEGRATION.md` | Frontend API and WebSocket integration |
| `13-COLLABORATION-UI.md` | Collaborative UI components |
| `14-CONFLICT-RESOLUTION.md` | Handling concurrent edits |
| `15-OFFLINE-MODE.md` | Offline support with IndexedDB fallback |
| `16-TESTING-DEPLOYMENT.md` | Testing strategy and deployment guide |

---

## 🏗️ What We're Building

### Current State (Frontend Only)
```
┌─────────────────────────────────────────┐
│           ChartDB Frontend              │
│  ┌─────────────────────────────────┐    │
│  │    React + TypeScript + Vite    │    │
│  │    Canvas-based ERD Editor      │    │
│  └─────────────────────────────────┘    │
│                  │                      │
│                  ▼                      │
│  ┌─────────────────────────────────┐    │
│  │         IndexedDB               │    │
│  │    (Local Storage Only)         │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### Target State (Full-Stack Collaborative)
```
┌─────────────────────────────────────────────────────────────────┐
│                    ChartDB Full-Stack                           │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   User A     │  │   User B     │  │   User C     │          │
│  │  (Browser)   │  │  (Browser)   │  │  (Browser)   │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│         │    WebSocket (STOMP over SockJS)  │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │                                     │
│                           ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Spring Boot Backend                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │   │
│  │  │  REST API   │ │  WebSocket  │ │  JWT Security   │    │   │
│  │  │ Controllers │ │  Handlers   │ │  Filter Chain   │    │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘    │   │
│  │                           │                              │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │              Service Layer                       │    │   │
│  │  │  (Business Logic, Validation, Conflict Res.)    │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                           │                              │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │           Spring Data JPA Repositories           │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └───────────────────────────┼─────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    PostgreSQL                            │   │
│  │  (Diagrams, Tables, Columns, Relationships, Users)      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Redis (Optional)                      │   │
│  │  (Session Cache, Presence, Cursor Positions)            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Features to Implement

### 1. Real-Time Collaboration
- **Live Cursors:** See other users' cursor positions with names/colors
- **Live Editing:** All changes sync instantly across all connected clients
- **Presence Indicators:** See who's currently viewing/editing the diagram
- **User Avatars:** Display user avatars at their cursor positions

### 2. Backend Persistence
- **PostgreSQL Database:** Store all diagrams, tables, columns, relationships
- **Position Persistence:** Save exact X/Y coordinates of every table
- **Version History:** Track changes with timestamps and user attribution
- **Auto-Save:** Automatically persist changes as users edit

### 3. Authentication & Authorization
- **JWT Authentication:** Secure token-based authentication
- **Role-Based Access:** Owner, Editor, Viewer permissions
- **Diagram Sharing:** Share diagrams with specific users or make public
- **Team Workspaces:** Organize diagrams by team/organization

### 4. Conflict Resolution
- **Operational Transform (OT):** Handle concurrent edits gracefully
- **Last-Write-Wins (LWW):** For position updates
- **Merge Strategies:** For complex structural changes
- **Conflict Notifications:** Alert users of conflicts

### 5. Offline Support
- **IndexedDB Fallback:** Continue working offline
- **Sync Queue:** Queue changes when offline
- **Auto-Reconnect:** Seamlessly reconnect and sync
- **Conflict Resolution:** Handle offline/online merge conflicts

### 6. Performance Optimizations
- **Debounced Updates:** Don't spam server on every pixel movement
- **Optimistic UI:** Update UI immediately, sync in background
- **Delta Sync:** Only send changed data, not entire diagram
- **Lazy Loading:** Load large diagrams progressively

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| Spring Boot 3.2+ | Main framework |
| Spring Web | REST API |
| Spring Data JPA | Database ORM |
| Spring WebSocket | Real-time communication |
| Spring Security | Authentication/Authorization |
| PostgreSQL 15+ | Primary database |
| Redis 7+ | Caching & presence (optional) |
| Lombok | Reduce boilerplate |
| MapStruct | DTO mapping |
| Flyway | Database migrations |

### Frontend (Existing + Additions)
| Technology | Purpose |
|------------|---------|
| React 18+ | UI framework (existing) |
| TypeScript | Type safety (existing) |
| Vite | Build tool (existing) |
| Zustand/Redux | State management (existing) |
| SockJS + STOMP | WebSocket client (NEW) |
| Axios | HTTP client (NEW) |
| React Query | Server state management (NEW) |

---

## 📁 Final Directory Structure

```
chartdb-fullstack/
├── backend/                          # Spring Boot Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/chartdb/
│   │   │   │   ├── ChartDbApplication.java
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── exception/
│   │   │   │   ├── mapper/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   ├── service/
│   │   │   │   └── websocket/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/
│   │   └── test/
│   ├── pom.xml
│   └── README.md
│
├── frontend/                         # React Application (copied from chartdb)
│   ├── src/
│   │   ├── components/
│   │   │   └── collaboration/        # NEW: Collaboration UI
│   │   ├── contexts/
│   │   │   └── AuthContext.tsx       # NEW: Auth context
│   │   ├── hooks/
│   │   │   ├── useWebSocket.ts       # NEW: WebSocket hook
│   │   │   └── useCollaboration.ts   # NEW: Collaboration hook
│   │   ├── services/
│   │   │   ├── api.ts                # NEW: API service
│   │   │   ├── authService.ts        # NEW: Auth service
│   │   │   └── websocketService.ts   # NEW: WebSocket service
│   │   ├── pages/
│   │   │   ├── Login.tsx             # NEW: Login page
│   │   │   └── Register.tsx          # NEW: Register page
│   │   └── ... (existing code)
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml                # Local development setup
├── docker-compose.prod.yml           # Production setup
└── README.md                         # Project documentation
```

---

## 🚀 Success Criteria

| Criteria | Description |
|----------|-------------|
| ✅ Backend Running | Spring Boot on port 8080 |
| ✅ Frontend Running | React on port 5173 |
| ✅ Database Connected | PostgreSQL storing all data |
| ✅ Real-time Sync | Changes sync within 100ms |
| ✅ Multi-user Cursors | See other users' cursors |
| ✅ Collaborative Editing | Multiple users edit simultaneously |
| ✅ Position Persistence | Table X/Y coordinates saved correctly |
| ✅ Authentication | JWT login/register working |
| ✅ Permissions | Owner/Editor/Viewer roles enforced |
| ✅ Offline Mode | Works without network |
| ✅ Conflict Handling | No data loss on concurrent edits |
| ✅ Performance | 60fps drag operations |
| ✅ Auto-Reconnect | Handles network interruptions |

---

## 📖 How to Use These Prompts

1. **Read sequentially:** Start with `01-EXISTING-ANALYSIS.md` and proceed in order
2. **Implement in phases:** Each file represents a logical implementation phase
3. **Copy code sections:** Each file contains production-ready code snippets
4. **Customize as needed:** Adjust configurations for your specific environment
5. **Test incrementally:** Test each phase before moving to the next

---

## ⚠️ Important Notes

1. **DO NOT recreate the frontend** - Only add integration layers
2. **ALWAYS save position_x and position_y** - Critical for diagram layout
3. **Use DTOs** - Never expose JPA entities directly in APIs
4. **Debounce updates** - Don't spam server on drag operations
5. **Handle offline gracefully** - Keep IndexedDB as fallback
6. **Test with multiple clients** - Real-time features need multi-client testing

---

**Next:** Continue to `01-EXISTING-ANALYSIS.md` →
