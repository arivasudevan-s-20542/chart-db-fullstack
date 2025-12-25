# 16 - Implementation Checklist

## ✅ Complete Implementation Checklist

Use this checklist to track progress during implementation.

---

## Phase 1: Project Setup

### Backend Setup
- [ ] Initialize Spring Boot project with required dependencies
- [ ] Configure `pom.xml` with all dependencies:
  - [ ] Spring Boot Starter Web
  - [ ] Spring Boot Starter Data JPA
  - [ ] Spring Boot Starter WebSocket
  - [ ] Spring Boot Starter Security
  - [ ] Spring Boot Starter Validation
  - [ ] PostgreSQL Driver
  - [ ] Lombok
  - [ ] MapStruct
  - [ ] JJWT (io.jsonwebtoken)
- [ ] Create package structure:
  ```
  com.chartdb
  ├── config/
  ├── controller/
  ├── dto/
  │   ├── request/
  │   └── response/
  ├── entity/
  ├── exception/
  ├── mapper/
  ├── repository/
  ├── security/
  ├── service/
  └── websocket/
      └── message/
  ```
- [ ] Configure `application.yml` with profiles (dev, test, production)
- [ ] Set up Docker Compose for local development

### Database Setup
- [ ] Create PostgreSQL database
- [ ] Create schema migration files (Flyway/Liquibase)
- [ ] Implement all entities:
  - [ ] User
  - [ ] Diagram
  - [ ] Table
  - [ ] Column
  - [ ] Relationship
  - [ ] DiagramCollaborator
  - [ ] ActiveCollaborator

---

## Phase 2: Security Layer

### JWT Implementation
- [ ] Create `JwtTokenProvider` class
- [ ] Create `UserPrincipal` class
- [ ] Create `JwtAuthenticationFilter`
- [ ] Create `JwtAuthenticationEntryPoint`
- [ ] Create `CustomUserDetailsService`
- [ ] Configure `SecurityConfig`
- [ ] Create `@CurrentUser` annotation
- [ ] Implement token refresh mechanism

### Authentication Endpoints
- [ ] POST `/auth/register`
- [ ] POST `/auth/login`
- [ ] POST `/auth/refresh`
- [ ] POST `/auth/logout`
- [ ] GET `/auth/me`
- [ ] POST `/auth/forgot-password` (optional)
- [ ] POST `/auth/reset-password` (optional)

---

## Phase 3: Core REST APIs

### User API
- [ ] UserRepository
- [ ] UserService
- [ ] UserMapper
- [ ] UserController
  - [ ] GET `/users/me`
  - [ ] PUT `/users/me`

### Diagram API
- [ ] DiagramRepository
- [ ] DiagramService
- [ ] DiagramMapper
- [ ] DiagramController
  - [ ] GET `/diagrams`
  - [ ] POST `/diagrams`
  - [ ] GET `/diagrams/{id}`
  - [ ] PUT `/diagrams/{id}`
  - [ ] DELETE `/diagrams/{id}`
  - [ ] GET `/diagrams/public/{id}`

### Table API
- [ ] TableRepository
- [ ] TableService
- [ ] TableMapper
- [ ] TableController
  - [ ] POST `/diagrams/{diagramId}/tables`
  - [ ] GET `/tables/{id}`
  - [ ] PUT `/tables/{id}`
  - [ ] PATCH `/tables/{id}/position`
  - [ ] DELETE `/tables/{id}`

### Column API
- [ ] ColumnRepository
- [ ] ColumnService
- [ ] ColumnMapper
- [ ] ColumnController
  - [ ] POST `/tables/{tableId}/columns`
  - [ ] PUT `/columns/{id}`
  - [ ] DELETE `/columns/{id}`
  - [ ] PUT `/tables/{tableId}/columns/reorder`

### Relationship API
- [ ] RelationshipRepository
- [ ] RelationshipService
- [ ] RelationshipMapper
- [ ] RelationshipController
  - [ ] POST `/diagrams/{diagramId}/relationships`
  - [ ] DELETE `/relationships/{id}`

---

## Phase 4: WebSocket & Real-time

### WebSocket Configuration
- [ ] Create `WebSocketConfig`
- [ ] Create `WebSocketAuthInterceptor`
- [ ] Create `WebSocketHandshakeInterceptor`
- [ ] Create `WebSocketEventListener`
- [ ] Configure CORS for WebSocket

### Message Classes
- [ ] CursorMoveMessage
- [ ] TableMoveMessage
- [ ] TableCreateMessage
- [ ] TableUpdateMessage
- [ ] TableDeleteMessage
- [ ] ColumnMessage
- [ ] JoinDiagramMessage
- [ ] LeaveDiagramMessage
- [ ] LockMessage
- [ ] SelectionMessage
- [ ] ErrorMessage
- [ ] LockResult

### Collaboration Service
- [ ] CollaborationService
- [ ] ActiveCollaboratorRepository
- [ ] Cursor color assignment
- [ ] Table locking mechanism
- [ ] Stale collaborator cleanup scheduler

### WebSocket Controller
- [ ] `/app/join-diagram`
- [ ] `/app/leave-diagram`
- [ ] `/app/cursor-move`
- [ ] `/app/table-move`
- [ ] `/app/table-create`
- [ ] `/app/table-update`
- [ ] `/app/table-delete`
- [ ] `/app/column-change`
- [ ] `/app/lock-table`
- [ ] `/app/unlock-table`
- [ ] `/app/select`

---

## Phase 5: Frontend Integration

### WebSocket Service
- [ ] Create `websocket.service.ts`
- [ ] STOMP client setup
- [ ] Connection management
- [ ] Reconnection logic
- [ ] Event handlers

### React Context
- [ ] Create `CollaborationContext`
- [ ] Collaborator state management
- [ ] Cursor tracking
- [ ] Lock state management

### Components
- [ ] CollaboratorCursor component
- [ ] CollaboratorCursorsOverlay
- [ ] CollaboratorAvatars
- [ ] Lock indicator on tables

### Hooks
- [ ] `useCollaboration` hook
- [ ] `useRealtimeSync` hook
- [ ] Throttled cursor updates

### API Service
- [ ] `api.service.ts` with axios
- [ ] Token refresh interceptor
- [ ] All API methods

### State Management Updates
- [ ] Update diagram store for real-time
- [ ] Conflict resolution strategy
- [ ] Optimistic updates

---

## Phase 6: DTOs & Validation

### Request DTOs
- [ ] UserRegistrationRequest (with validation)
- [ ] UserLoginRequest
- [ ] CreateDiagramRequest
- [ ] UpdateDiagramRequest
- [ ] CreateTableRequest
- [ ] UpdateTableRequest
- [ ] TablePositionUpdate
- [ ] BulkTablePositionUpdate
- [ ] CreateColumnRequest
- [ ] UpdateColumnRequest
- [ ] ReorderColumnsRequest
- [ ] CreateRelationshipRequest
- [ ] UpdateRelationshipRequest

### Response DTOs
- [ ] AuthResponse
- [ ] UserDTO
- [ ] DiagramDTO
- [ ] DiagramSummaryDTO
- [ ] TableDTO
- [ ] ColumnDTO
- [ ] RelationshipDTO
- [ ] CollaboratorDTO
- [ ] PageResponse<T>
- [ ] ErrorResponse

### MapStruct Mappers
- [ ] UserMapper
- [ ] DiagramMapper
- [ ] TableMapper
- [ ] ColumnMapper
- [ ] RelationshipMapper

---

## Phase 7: Testing

### Unit Tests
- [ ] Service layer tests
- [ ] Mapper tests
- [ ] Security tests

### Integration Tests
- [ ] REST API tests with MockMvc
- [ ] WebSocket tests
- [ ] Repository tests with Testcontainers

### Test Configuration
- [ ] `application-test.yml`
- [ ] `AbstractIntegrationTest` base class
- [ ] Test data factories

---

## Phase 8: DevOps & Deployment

### Docker
- [ ] Backend Dockerfile
- [ ] Frontend Dockerfile
- [ ] docker-compose.yml (dev)
- [ ] docker-compose.prod.yml
- [ ] nginx.conf

### CI/CD
- [ ] GitHub Actions workflow
- [ ] Test job
- [ ] Build job
- [ ] Deploy job

### Monitoring
- [ ] Actuator endpoints
- [ ] Prometheus metrics
- [ ] Health checks

---

## Phase 9: Documentation

### API Documentation
- [ ] OpenAPI/Swagger setup
- [ ] API endpoint documentation
- [ ] Request/Response examples

### README
- [ ] Project overview
- [ ] Setup instructions
- [ ] Development guide
- [ ] Deployment guide

---

## 🎯 Success Criteria

### Functional Requirements
- [ ] Users can register and login
- [ ] Users can create, edit, delete diagrams
- [ ] Users can add/edit/delete tables and columns
- [ ] Users can create relationships between tables
- [ ] Multiple users can view/edit same diagram simultaneously
- [ ] User cursors are visible to collaborators
- [ ] Tables can be locked during editing
- [ ] Changes sync in real-time across clients

### Non-Functional Requirements
- [ ] WebSocket reconnects automatically on disconnect
- [ ] API responses < 200ms for simple operations
- [ ] Real-time updates < 100ms latency
- [ ] Supports 50+ concurrent users per diagram
- [ ] All endpoints have proper authentication
- [ ] Proper error handling and messages
- [ ] 80%+ test coverage

---

## 📝 Notes

### Known Considerations
1. For production, consider Redis for WebSocket session storage
2. Implement rate limiting for API and WebSocket messages
3. Add database connection pooling (HikariCP is default)
4. Consider implementing soft deletes
5. Add audit logging for security-sensitive operations
6. Implement proper database backups

### Future Enhancements
- [ ] OAuth2 login (Google, GitHub)
- [ ] Diagram versioning/history
- [ ] Export to SQL/PNG/SVG
- [ ] Import from existing databases
- [ ] Comments/annotations on diagrams
- [ ] Diagram templates
- [ ] Team/organization workspaces
- [ ] Presence indicators (typing, viewing)
- [ ] Undo/redo with operational transformation
- [ ] Offline support with sync

---

**← Previous:** `15-DEPLOYMENT.md` | **Index:** `00-OVERVIEW.md`
