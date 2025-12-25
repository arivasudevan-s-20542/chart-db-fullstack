# 🚀 ERD Tool - Complete Setup Guide

## 📦 What's Been Created

Your ERD Tool application now consists of:

### 1️⃣ **Frontend** (`erdtool-frontend/`)
- ✅ React 18 + TypeScript + Vite
- ✅ Zoho IAM OAuth 2.0 authentication
- ✅ Protected routes
- ✅ API client with automatic token refresh
- ✅ Login and callback pages
- ✅ Tailwind CSS styling

### 2️⃣ **Backend** (`erdtool-backend/`)
- ✅ Spring Boot 3.2.0
- ✅ PostgreSQL database integration
- ✅ JWT + Zoho OAuth authentication
- ✅ User management
- ✅ Diagram CRUD endpoints
- ✅ Flyway migrations

---

## 🎯 Quick Start (5 Minutes)

### Step 1: Set Up Zoho OAuth

1. Go to [Zoho API Console](https://api-console.zoho.com/)
2. Click **"Add Client"** → **"Server-based Applications"**
3. Fill in:
   - **Client Name:** ERD Tool
   - **Homepage URL:** `http://localhost:3000`
   - **Redirect URI:** `http://localhost:3000/auth/callback`
4. Add scope: `AaaServer.profile.READ`
5. Copy **Client ID** and **Client Secret**

### Step 2: Configure Backend

Create `erdtool-backend/.env`:

```bash
cat > /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend/.env << 'EOF'
DATABASE_URL=jdbc:postgresql://localhost:5432/erdtool
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=$(openssl rand -base64 32)
ZOHO_CLIENT_ID=YOUR_CLIENT_ID_HERE
ZOHO_CLIENT_SECRET=YOUR_CLIENT_SECRET_HERE
ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
CORS_ALLOWED_ORIGINS=http://localhost:3000
FRONTEND_URL=http://localhost:3000
PORT=8080
EOF
```

**Replace** `YOUR_CLIENT_ID_HERE` and `YOUR_CLIENT_SECRET_HERE` with values from Step 1.

### Step 3: Configure Frontend

Create `erdtool-frontend/.env`:

```bash
cat > /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend/.env << 'EOF'
VITE_ZOHO_CLIENT_ID=YOUR_CLIENT_ID_HERE
VITE_ZOHO_REDIRECT_URI=http://localhost:3000/auth/callback
VITE_ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
VITE_API_BASE_URL=/api
EOF
```

**Replace** `YOUR_CLIENT_ID_HERE` with the same Client ID from Step 1.

### Step 4: Start PostgreSQL

```bash
# If PostgreSQL is not running:
sudo systemctl start postgresql

# Create database:
psql -U postgres -c "CREATE DATABASE erdtool;"
psql -U postgres -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```

### Step 5: Start Backend

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend
mvn clean spring-boot:run
```

**Wait for:** `Started ErdToolApplication in X.XXX seconds`

### Step 6: Start Frontend

**Open a new terminal:**

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend
npm install
npm run dev
```

**Wait for:** `Local: http://localhost:3000/`

### Step 7: Test Authentication

1. Open browser: `http://localhost:3000`
2. Click **"Sign in with Zoho"**
3. Authorize the application
4. You should be redirected back and logged in! ✅

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │  React Frontend (localhost:3000)                   │     │
│  │  - Login Page                                      │     │
│  │  - OAuth Callback Handler                          │     │
│  │  - Protected Routes                                │     │
│  │  - API Client (with auto token refresh)            │     │
│  └────────────┬───────────────────────────────────────┘     │
└───────────────┼─────────────────────────────────────────────┘
                │
                │ HTTP Requests (Bearer Token)
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│         Spring Boot Backend (localhost:8080)                │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Controllers                                       │     │
│  │  - ZohoAuthController (/api/auth/zoho/*)          │     │
│  │  - DiagramController (/api/diagrams/*)            │     │
│  │  - UserController (/api/users/*)                  │     │
│  └────────────┬───────────────────────────────────────┘     │
│               │                                             │
│  ┌────────────▼───────────────────────────────────────┐     │
│  │  Services                                          │     │
│  │  - ZohoAuthService (OAuth token exchange)         │     │
│  │  - DiagramService (Business logic)                │     │
│  │  - UserService (User management)                  │     │
│  └────────────┬───────────────────────────────────────┘     │
│               │                                             │
│  ┌────────────▼───────────────────────────────────────┐     │
│  │  Repositories (JPA)                                │     │
│  │  - UserRepository                                  │     │
│  │  - DiagramRepository                               │     │
│  └────────────┬───────────────────────────────────────┘     │
└───────────────┼─────────────────────────────────────────────┘
                │
                │ JDBC
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│         PostgreSQL Database (localhost:5432)                │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Tables                                            │     │
│  │  - users                                           │     │
│  │  - diagrams                                        │     │
│  │  - flyway_schema_history                           │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘

                ▲
                │
                │ OAuth 2.0 Flow
                │
┌───────────────┴─────────────────────────────────────────────┐
│         Zoho IAM (accounts.zoho.com)                        │
│  - User Authentication                                      │
│  - Authorization                                            │
│  - Token Issuance                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Flow

```
1. User → Frontend: Click "Sign in with Zoho"
2. Frontend → Zoho: Redirect to authorization page
3. User → Zoho: Authorize application
4. Zoho → Frontend: Redirect with authorization code
5. Frontend → Backend: POST /api/auth/zoho/token (with code)
6. Backend → Zoho: Exchange code for access token
7. Zoho → Backend: Return access_token + refresh_token
8. Backend → Frontend: Return tokens
9. Frontend → Backend: GET /api/auth/zoho/userinfo (with token)
10. Backend → Zoho: Fetch user profile
11. Zoho → Backend: Return user data
12. Backend → Frontend: Return user info
13. Frontend: Store tokens + redirect to dashboard
```

---

## 📂 Project Structure

```
PLAYBOOKS_DATASPACE/
│
├── erdtool-frontend/              # React Frontend
│   ├── components/
│   │   └── ProtectedRoute.tsx     # Route guard
│   ├── lib/
│   │   ├── zoho-auth.ts           # OAuth service
│   │   └── api-client.ts          # Backend API client
│   ├── pages/
│   │   ├── LoginPage.tsx          # Login UI
│   │   └── AuthCallbackPage.tsx   # OAuth callback handler
│   ├── public/                    # Static assets
│   ├── .env                       # Frontend config
│   ├── vite.config.ts             # Vite config
│   └── package.json               # Dependencies
│
├── erdtool-backend/               # Spring Boot Backend
│   ├── src/main/java/com/erdtool/
│   │   ├── controller/
│   │   │   ├── ZohoAuthController.java    # OAuth endpoints
│   │   │   ├── DiagramController.java     # Diagram CRUD
│   │   │   └── UserController.java        # User management
│   │   ├── service/
│   │   │   ├── ZohoAuthService.java       # OAuth logic
│   │   │   ├── DiagramService.java
│   │   │   └── UserService.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   └── DiagramRepository.java
│   │   ├── dto/
│   │   │   ├── ZohoTokenRequest.java
│   │   │   ├── ZohoTokenResponse.java
│   │   │   └── ZohoUserInfo.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   └── RestTemplateConfig.java
│   │   └── security/
│   │       ├── JwtAuthenticationFilter.java
│   │       └── CustomUserDetailsService.java
│   ├── src/main/resources/
│   │   ├── application.yml        # Main config
│   │   └── db/migration/          # Flyway migrations
│   │       ├── V0__Enable_pgcrypto.sql
│   │       ├── V1__Create_users_table.sql
│   │       └── V2__Create_diagrams_table.sql
│   ├── .env                       # Backend config
│   └── pom.xml                    # Maven dependencies
│
└── ZOHO_IAM_SETUP.md              # Detailed setup guide
```

---

## 🔧 Configuration Files

### Frontend Environment Variables

```env
# .env (erdtool-frontend/)
VITE_ZOHO_CLIENT_ID=1000.XXXXXXXXXXXXX
VITE_ZOHO_REDIRECT_URI=http://localhost:3000/auth/callback
VITE_ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
VITE_API_BASE_URL=/api
```

### Backend Environment Variables

```env
# .env (erdtool-backend/)
DATABASE_URL=jdbc:postgresql://localhost:5432/erdtool
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=generated-secure-key
ZOHO_CLIENT_ID=1000.XXXXXXXXXXXXX
ZOHO_CLIENT_SECRET=xxxxxxxxxxxxx
ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
CORS_ALLOWED_ORIGINS=http://localhost:3000
FRONTEND_URL=http://localhost:3000
PORT=8080
```

---

## 🛠️ Development Workflow

### Terminal 1: Backend Development

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend

# Run with hot reload
mvn spring-boot:run

# Or for debugging:
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Terminal 2: Frontend Development

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend

# Start dev server with hot reload
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Terminal 3: Database Management

```bash
# Connect to database
psql -U postgres -d erdtool

# View tables
\dt

# View users
SELECT * FROM users;

# View diagrams
SELECT * FROM diagrams;

# View migration history
SELECT * FROM flyway_schema_history;
```

---

## 🧪 Testing the Integration

### 1. Test Authentication

```bash
# Open in browser:
http://localhost:3000

# Expected flow:
1. See login page
2. Click "Sign in with Zoho"
3. Redirected to Zoho (accounts.zoho.com)
4. Enter credentials
5. Authorize application
6. Redirected back to app
7. Logged in successfully ✅
```

### 2. Test API Calls

```bash
# Get access token from browser localStorage
TOKEN="your_access_token_here"

# Test user endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me

# Test diagrams endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/diagrams
```

### 3. Test Token Refresh

```bash
# The frontend automatically refreshes tokens
# To test manually:

REFRESH_TOKEN="your_refresh_token"

curl -X POST http://localhost:8080/api/auth/zoho/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\": \"$REFRESH_TOKEN\"}"
```

---

## 🐛 Common Issues & Solutions

### Issue: "Port 3000 already in use"

```bash
# Find and kill process
lsof -ti:3000 | xargs kill -9

# Or use different port
npm run dev -- --port 3001
```

### Issue: "Port 8080 already in use"

```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9

# Or change in .env
PORT=8081
```

### Issue: "Database connection refused"

```bash
# Start PostgreSQL
sudo systemctl start postgresql

# Create database if missing
psql -U postgres -c "CREATE DATABASE erdtool;"
```

### Issue: "Invalid redirect_uri"

**Solution:** Ensure Zoho API Console redirect URI **exactly matches**:
```
http://localhost:3000/auth/callback
```

### Issue: "CORS error"

**Solution:** 
1. Check `CORS_ALLOWED_ORIGINS` in backend `.env`
2. Restart backend: `mvn spring-boot:run`
3. Clear browser cache

### Issue: "gen_random_uuid() does not exist"

**Solution:** Already fixed! The `V0__Enable_pgcrypto.sql` migration enables it.

---

## 📊 Database Schema

### Users Table

```sql
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    username            VARCHAR(100) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    avatar_url          TEXT,
    auth_provider       VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    auth_provider_id    VARCHAR(255),
    is_anonymous        BOOLEAN DEFAULT false,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at       TIMESTAMP,
    is_active           BOOLEAN DEFAULT true
);
```

### Diagrams Table

```sql
CREATE TABLE diagrams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    content         TEXT NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 Deployment to Production

### Backend (Spring Boot)

1. Build JAR:
   ```bash
   mvn clean package -DskipTests
   ```

2. Run JAR:
   ```bash
   java -jar target/erdtool-backend-1.0.0.jar
   ```

3. Or use Docker:
   ```dockerfile
   FROM eclipse-temurin:17-jdk-alpine
   COPY target/*.jar app.jar
   ENTRYPOINT ["java","-jar","/app.jar"]
   ```

### Frontend (React)

1. Build:
   ```bash
   npm run build
   ```

2. Deploy `dist/` folder to:
   - Nginx
   - Apache
   - Vercel
   - Netlify
   - AWS S3 + CloudFront

### Environment Variables (Production)

Update redirect URIs in:
- Zoho API Console
- Frontend `.env.production`
- Backend `application.yml`

Example:
```
VITE_ZOHO_REDIRECT_URI=https://yourdomain.com/auth/callback
```

---

## ✅ Final Checklist

- [ ] Zoho OAuth app created
- [ ] Client ID and Secret configured
- [ ] PostgreSQL database created
- [ ] Backend `.env` configured
- [ ] Frontend `.env` configured
- [ ] Backend running on port 8080
- [ ] Frontend running on port 3000
- [ ] Can access login page
- [ ] Can sign in with Zoho
- [ ] Token refresh works
- [ ] API calls authenticated
- [ ] Database migrations applied

---

## 🎉 Success!

Your ERD Tool application is now fully integrated with:
- ✅ Zoho IAM authentication
- ✅ Secure token management
- ✅ Frontend-backend communication
- ✅ PostgreSQL persistence
- ✅ Auto token refresh
- ✅ Protected routes

**Next steps:**
1. Customize the UI
2. Add diagram creation/editing features
3. Implement collaboration features
4. Deploy to production

---

## 📞 Need Help?

- **Zoho OAuth:** [Official Documentation](https://www.zoho.com/accounts/protocol/oauth.html)
- **Spring Boot:** [Spring Security OAuth](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- **React:** [React Docs](https://react.dev/)

Happy coding! 🚀
