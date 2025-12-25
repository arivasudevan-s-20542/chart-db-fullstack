# ChartDB Fullstack

A real-time collaborative ERD (Entity-Relationship Diagram) tool with Spring Boot backend and React frontend.

## 🚀 Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.2**
- **PostgreSQL 15** - Primary database
- **Redis 7** - Caching and session management
- **WebSocket** - Real-time collaboration
- **JWT** - Authentication

### Frontend
- **React** with **TypeScript**
- **Vite** - Build tool
- **TailwindCSS** - Styling

## 📋 Prerequisites

- Docker & Docker Compose
- Node.js 20+ (for local frontend development)
- Java 21 (for local backend development)
- Maven 3.9+

## 🛠️ Local Development

### Using Docker Compose (Recommended)

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop services
docker compose down
```

### Manual Setup

**Backend:**
```bash
cd backend
export JAVA_HOME=/path/to/jdk21
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## 🌐 Access

- **Frontend:** http://localhost:5173 (dev) or http://localhost:80 (production)
- **Backend API:** http://localhost:8080
- **WebSocket:** ws://localhost:8080/ws

## 📦 Deployment

This project uses GitHub Actions for CI/CD. On push to `main`:

1. Tests run for both backend and frontend
2. Docker images are built and pushed to GitHub Container Registry
3. Images are deployed to the production server

### Required GitHub Secrets

Configure these in your repository settings:

| Secret | Description |
|--------|-------------|
| `SERVER_HOST` | Your server IP or hostname |
| `SERVER_USER` | SSH username |
| `SERVER_SSH_KEY` | Private SSH key for deployment |

### Required GitHub Variables

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Backend API URL (e.g., `https://api.yourdomain.com`) |
| `VITE_WS_URL` | WebSocket URL (e.g., `wss://api.yourdomain.com/ws`) |

### Server Setup

1. Install Docker and Docker Compose on your server
2. Copy `.env.example` to `.env` and configure:
   ```bash
   cp .env.example .env
   nano .env
   ```
3. The CI/CD pipeline will handle the rest!

## 🔐 Environment Variables

See [.env.example](.env.example) for all available configuration options.

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

