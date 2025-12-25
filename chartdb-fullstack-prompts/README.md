# ChartDB Fullstack Enhancement Prompt

## 📚 Document Index

This directory contains a comprehensive, enhanced prompt for implementing a Spring Boot backend with real-time collaboration features for ChartDB.

### Document Structure

| File | Description |
|------|-------------|
| `00-OVERVIEW.md` | Project overview, goals, feature matrix, and success criteria |
| `01-EXISTING-ANALYSIS.md` | Analysis of existing ChartDB React application structure |
| `02-ARCHITECTURE.md` | System architecture diagrams and component interactions |
| `03-DATABASE-SCHEMA.md` | Complete PostgreSQL schema with all tables and indexes |
| `04-BACKEND-STRUCTURE.md` | Spring Boot project setup and directory structure |
| `05-BACKEND-ENTITIES.md` | JPA entity classes with relationships |
| `06-BACKEND-REPOSITORIES.md` | Spring Data JPA repositories with custom queries |
| `07-BACKEND-SERVICES.md` | Service layer implementations |
| `08-BACKEND-CONTROLLERS.md` | REST API controllers and endpoints |
| `09-WEBSOCKET-CONFIG.md` | WebSocket + STOMP configuration and setup |
| `10-WEBSOCKET-HANDLERS.md` | WebSocket message handlers and collaboration service |
| `11-DTOS-MAPPERS.md` | Data Transfer Objects and MapStruct mappers |
| `12-FRONTEND-INTEGRATION.md` | React WebSocket service and collaboration context |
| `13-SECURITY-JWT.md` | JWT authentication and Spring Security configuration |
| `14-TESTING.md` | Testing strategy with unit and integration tests |
| `15-DEPLOYMENT.md` | Docker, CI/CD, and Kubernetes deployment |
| `16-IMPLEMENTATION-CHECKLIST.md` | Complete implementation checklist |

---

## 🚀 Quick Start

1. **Read the Overview** (`00-OVERVIEW.md`) to understand the project scope
2. **Review the Architecture** (`02-ARCHITECTURE.md`) for system design
3. **Follow the Database Schema** (`03-DATABASE-SCHEMA.md`) to set up PostgreSQL
4. **Implement Backend in Order**:
   - Structure (`04`)
   - Entities (`05`)
   - Repositories (`06`)
   - Services (`07`)
   - Controllers (`08`)
5. **Add WebSocket Support** (`09`, `10`)
6. **Integrate Frontend** (`12`)
7. **Secure with JWT** (`13`)
8. **Write Tests** (`14`)
9. **Deploy** (`15`)

---

## 🛠️ Technology Stack

### Backend
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Security**
- **Spring WebSocket + STOMP**
- **PostgreSQL 15+**
- **MapStruct** (DTO mapping)
- **Lombok** (boilerplate reduction)
- **JWT** (io.jsonwebtoken)

### Frontend (Existing + Additions)
- **React 18+**
- **TypeScript**
- **@stomp/stompjs** (WebSocket client)
- **SockJS** (WebSocket fallback)
- **Zustand/Redux** (state management)

### DevOps
- **Docker & Docker Compose**
- **GitHub Actions** (CI/CD)
- **Nginx** (reverse proxy)
- **Kubernetes** (optional)

---

## 📋 Key Features

1. **User Authentication**
   - JWT-based authentication
   - Secure password storage
   - Token refresh mechanism

2. **Diagram Management**
   - CRUD operations for diagrams
   - Table and column management
   - Relationship definitions

3. **Real-time Collaboration**
   - Live cursor tracking
   - Instant change synchronization
   - User presence indicators
   - Table locking mechanism

4. **Permission System**
   - Owner, editor, viewer roles
   - Public/private diagrams

---

## 💡 Usage Tips

- Each document is self-contained but references related documents
- Code blocks are complete and ready to use
- Follow the checklist in `16-IMPLEMENTATION-CHECKLIST.md` for systematic implementation
- Customize configurations based on your environment

---

## 📄 License

This prompt documentation is provided for implementation guidance. Adapt as needed for your specific requirements.
