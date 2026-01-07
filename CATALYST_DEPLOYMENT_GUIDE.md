# Zoho Catalyst Deployment Guide

This guide explains how to deploy ChartDB to Zoho Catalyst serverless platform.

## 📋 Prerequisites

1. **Zoho Catalyst Account** - Sign up at [catalyst.zoho.com](https://catalyst.zoho.com)
2. **Catalyst CLI** - Install globally: `npm install -g zcatalyst-cli`
3. **Node.js 20+** and **Java 21+** for local development

## 🚀 Quick Start

### 1. Create Catalyst Project

```bash
# Login to Catalyst CLI
catalyst auth:login

# Initialize project (run from project root)
cd chart-db-fullstack
catalyst init
```

When prompted:
- **Project Name:** chartdb-fullstack
- **Project Domain:** chartdb

### 2. Configure Database

Catalyst offers two database options:

#### Option A: Catalyst Cloud SQL (Recommended)
1. Go to Catalyst Console → Your Project → Cloud Scale → Cloud SQL
2. Create a PostgreSQL instance
3. Note the connection details
4. Set environment variables:
   ```bash
   catalyst env:set DATABASE_URL="jdbc:postgresql://<host>:<port>/<database>"
   catalyst env:set DATABASE_USERNAME="<username>"
   catalyst env:set DATABASE_PASSWORD="<password>"
   ```

#### Option B: External PostgreSQL
Connect to an external PostgreSQL database (e.g., AWS RDS, Azure Database):
```bash
catalyst env:set DATABASE_URL="jdbc:postgresql://<external-host>:<port>/<database>"
catalyst env:set DATABASE_USERNAME="<username>"
catalyst env:set DATABASE_PASSWORD="<password>"
```

### 3. Build & Deploy

```bash
# Build backend
cd backend
mvn clean package -DskipTests -Pcatalyst

# Copy JAR to Catalyst function
mkdir -p ../catalyst/functions/chartdb_backend/lib
cp target/*.jar ../catalyst/functions/chartdb_backend/lib/

# Build frontend
cd ../frontend
npm ci
npm run build

# Copy to Catalyst client
cp -r dist/* ../catalyst/client/

# Deploy
cd ../catalyst
catalyst deploy
```

### 4. Set Environment Variables

```bash
# Required
catalyst env:set JWT_SECRET="your-secure-jwt-secret-minimum-32-characters"
catalyst env:set SPRING_PROFILES_ACTIVE="catalyst"

# OAuth2 (Optional - for social login)
catalyst env:set GOOGLE_CLIENT_ID="your-google-client-id"
catalyst env:set GOOGLE_CLIENT_SECRET="your-google-client-secret"
catalyst env:set GITHUB_CLIENT_ID="your-github-client-id"
catalyst env:set GITHUB_CLIENT_SECRET="your-github-client-secret"
catalyst env:set ZOHO_CLIENT_ID="your-zoho-client-id"
catalyst env:set ZOHO_CLIENT_SECRET="your-zoho-client-secret"
```

## 📁 Project Structure for Catalyst

```
chart-db-fullstack/
├── catalyst/
│   ├── catalyst.json           # Catalyst project config
│   ├── functions/
│   │   └── chartdb_backend/
│   │       ├── catalyst-config.json
│   │       └── lib/
│   │           └── chartdb-backend-1.0.0-SNAPSHOT.jar
│   └── client/
│       ├── index.html
│       ├── assets/
│       └── ...
```

## 🔧 Configuration Files

### catalyst.json
Main project configuration defining:
- Project name and domain
- Functions (backend API)
- Client (frontend web app)

### catalyst-config.json
Function-specific configuration:
- Java runtime settings
- Memory allocation (heap_size)
- Execution timeout

### application-catalyst.yml
Spring Boot configuration for Catalyst environment.

## 🌐 URLs After Deployment

| Resource | URL |
|----------|-----|
| Web Client | `https://<project-domain>-<project-id>.catalyst.zoho.com` |
| API Endpoint | `https://<project-domain>-<project-id>.catalyst.zoho.com/server/chartdb_backend` |
| WebSocket | `wss://<project-domain>-<project-id>.catalyst.zoho.com/server/chartdb_backend/ws` |

## 🔐 GitHub Actions Deployment

### Required Secrets

Configure these in your GitHub repository settings (Settings → Secrets and variables → Actions):

| Secret | Description |
|--------|-------------|
| `CATALYST_PROJECT_ID` | Your Catalyst project ID |
| `CATALYST_ORG_ID` | Your Zoho organization ID |
| `CATALYST_REFRESH_TOKEN` | OAuth refresh token for CLI auth |
| `JWT_SECRET` | JWT signing secret (32+ chars) |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID (optional) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret (optional) |
| `GITHUB_CLIENT_ID` | GitHub OAuth client ID (optional) |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth client secret (optional) |
| `ZOHO_CLIENT_ID` | Zoho OAuth client ID (optional) |
| `ZOHO_CLIENT_SECRET` | Zoho OAuth client secret (optional) |

### Getting Catalyst Refresh Token

1. Login to Catalyst CLI: `catalyst auth:login`
2. Find the token file: `~/.zcatalyst/credentials.json`
3. Copy the `refresh_token` value
4. Add it as `CATALYST_REFRESH_TOKEN` secret in GitHub

## 📊 Monitoring & Logs

### View Logs
```bash
catalyst logs --function chartdb_backend --tail
```

### In Catalyst Console
1. Go to Catalyst Console → Your Project
2. Navigate to Monitor → Logs
3. Filter by function name

## ⚠️ Limitations & Considerations

### Function Limits
- **Execution Timeout:** 90 seconds (configurable up to 300s)
- **Memory:** 512MB default (configurable)
- **Payload Size:** 6MB max for request/response

### WebSocket Limitations
Catalyst Advanced I/O functions don't support persistent WebSocket connections natively. For real-time features:

1. **Polling Fallback:** Use HTTP polling for presence/cursor updates
2. **Zoho Catalyst Signals:** Use Catalyst's built-in Signals for real-time messaging
3. **External WebSocket:** Use a separate WebSocket service (e.g., Pusher, Ably)

### Database Considerations
- Catalyst Data Store has different APIs than SQL
- Cloud SQL or external PostgreSQL is recommended for this app
- Connection pooling is limited in serverless environment

## 🔄 Updating Deployment

### Manual Update
```bash
# Rebuild and redeploy
cd backend && mvn clean package -DskipTests -Pcatalyst
cp target/*.jar ../catalyst/functions/chartdb_backend/lib/
cd ../frontend && npm run build
cp -r dist/* ../catalyst/client/
cd ../catalyst && catalyst deploy
```

### Via GitHub Actions
Push to `feature/zoho-catalyst-deployment` branch or trigger the workflow manually.

## 🐛 Troubleshooting

### Cold Start Issues
Catalyst functions may have cold starts. The first request after idle period might take longer.

**Solution:** Increase `heap_size` in catalyst-config.json for faster startup.

### Database Connection Errors
```
SQLException: No SQL database configured
```
**Solution:** Set DATABASE_URL environment variable or configure Cloud SQL.

### CORS Issues
```
Access-Control-Allow-Origin header missing
```
**Solution:** Ensure CORS is configured in application-catalyst.yml.

### Memory Issues
```
OutOfMemoryError: Java heap space
```
**Solution:** Increase `heap_size` in catalyst-config.json (max 1024).

## 📚 Resources

- [Catalyst Documentation](https://catalyst.zoho.com/help/)
- [Catalyst CLI Reference](https://catalyst.zoho.com/help/cli-reference.html)
- [Advanced I/O Functions](https://catalyst.zoho.com/help/functions-advancedio.html)
- [Cloud SQL Setup](https://catalyst.zoho.com/help/cloudsql-getting-started.html)
