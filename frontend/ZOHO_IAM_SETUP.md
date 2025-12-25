# Zoho IAM Integration Setup Guide

## 🎯 Overview
This guide will help you set up Zoho IAM (Identity and Access Management) OAuth 2.0 authentication for the ERD Tool application.

---

## 📋 Prerequisites

- Zoho Developer Account (free)
- Zoho Organization/Workspace
- Admin access to configure OAuth applications

---

## 🔧 Step 1: Create Zoho OAuth Application

### 1.1 Access Zoho API Console

1. Go to [Zoho API Console](https://api-console.zoho.com/)
2. Sign in with your Zoho account
3. Click **"Add Client"** or **"Get Started"**

### 1.2 Register Your Application

Select **"Server-based Applications"** (for OAuth 2.0 authorization code flow)

Fill in the following details:

| Field | Value |
|-------|-------|
| **Client Name** | ERD Tool Application |
| **Homepage URL** | `http://localhost:3000` (for dev) |
| **Authorized Redirect URIs** | `http://localhost:3000/auth/callback` |

**For Production:**
- Homepage URL: `https://yourdomain.com`
- Redirect URI: `https://yourdomain.com/auth/callback`

### 1.3 Configure Scopes

Add the following scope:
- ✅ `AaaServer.profile.READ` - To read user profile information

Click **"CREATE"**

### 1.4 Get Your Credentials

After creation, you'll receive:
- ✅ **Client ID** (e.g., `1000.XXXXXXXXXXXXXX`)
- ✅ **Client Secret** (keep this secure!)

---

## 🔐 Step 2: Configure Backend (Spring Boot)

### 2.1 Create Environment File

Create `/home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend/.env`:

```env
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/erdtool
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=change-this-to-a-secure-random-256-bit-key-in-production

# Zoho OAuth Configuration
ZOHO_CLIENT_ID=1000.XXXXXXXXXXXXXXXXXXXXX
ZOHO_CLIENT_SECRET=your_client_secret_here
ZOHO_ACCOUNTS_URL=https://accounts.zoho.com

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Frontend URL
FRONTEND_URL=http://localhost:3000

# Server Port
PORT=8080
```

### 2.2 Update Security Configuration (if needed)

The `ZohoAuthController` is already configured to accept CORS from the frontend.

---

## 🎨 Step 3: Configure Frontend (React + Vite)

### 3.1 Create Environment File

Create `/home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend/.env`:

```env
# Zoho OAuth Configuration
VITE_ZOHO_CLIENT_ID=1000.XXXXXXXXXXXXXXXXXXXXX
VITE_ZOHO_REDIRECT_URI=http://localhost:3000/auth/callback
VITE_ZOHO_ACCOUNTS_URL=https://accounts.zoho.com

# Backend API
VITE_API_BASE_URL=/api
```

---

## 🚀 Step 4: Start the Applications

### 4.1 Start Backend

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend
mvn spring-boot:run
```

Backend will run on: **http://localhost:8080**

### 4.2 Start Frontend

```bash
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend
npm install
npm run dev
```

Frontend will run on: **http://localhost:3000**

---

## 🔍 Step 5: Test Authentication Flow

### 5.1 Access the Application

1. Open browser: `http://localhost:3000`
2. You should see the **Login Page**
3. Click **"Sign in with Zoho"**

### 5.2 OAuth Flow Steps

1. **Redirect to Zoho**: User is redirected to `accounts.zoho.com`
2. **User Authorization**: User logs in and authorizes the app
3. **Callback with Code**: Zoho redirects back with authorization code
4. **Token Exchange**: Backend exchanges code for access token
5. **Fetch User Info**: Backend fetches user profile
6. **Save Session**: Frontend stores tokens and redirects to dashboard

### 5.3 Expected User Flow

```
Login Page (localhost:3000/login)
     ↓
Zoho Login (accounts.zoho.com)
     ↓
Authorization Grant
     ↓
Callback (localhost:3000/auth/callback?code=xxx&state=xxx)
     ↓
Token Exchange (Backend API)
     ↓
Dashboard (localhost:3000/)
```

---

## 🛠️ Step 6: Verify Integration

### 6.1 Check Backend Logs

Look for successful authentication:
```
INFO  c.e.s.ZohoAuthService : Successfully exchanged authorization code
INFO  c.e.s.ZohoAuthService : User authenticated: user@example.com
```

### 6.2 Check Browser Console

No errors should appear. Check:
- ✅ Access token stored in localStorage
- ✅ User info cached
- ✅ API calls include `Authorization: Bearer xxx` header

### 6.3 Test API Calls

Open browser DevTools → Network tab:
- Check `/api/auth/zoho/token` returns 200 OK
- Check `/api/auth/zoho/userinfo` returns user data
- Check `/api/diagrams` calls include Bearer token

---

## 🔒 Security Best Practices

### Production Checklist

- [ ] Change `JWT_SECRET` to a cryptographically secure 256-bit key
- [ ] Use HTTPS for all endpoints
- [ ] Set `CORS_ALLOWED_ORIGINS` to production domain
- [ ] Store `ZOHO_CLIENT_SECRET` in secure vault (AWS Secrets Manager, HashiCorp Vault)
- [ ] Enable rate limiting on auth endpoints
- [ ] Implement refresh token rotation
- [ ] Add audit logging for authentication events
- [ ] Configure session timeouts
- [ ] Enable CSRF protection

### Token Management

```typescript
// Frontend automatically handles:
✅ Token refresh before expiry
✅ Retry failed requests after token refresh
✅ Auto-logout on refresh failure
✅ State verification (CSRF protection)
```

---

## 🐛 Troubleshooting

### Issue: "Invalid redirect_uri"

**Solution:** Ensure the redirect URI in Zoho API Console **exactly matches** the one in `.env`

### Issue: "Invalid client_id"

**Solution:** Copy-paste the Client ID from Zoho API Console (including the `1000.` prefix)

### Issue: "CORS error"

**Solution:** 
1. Check `CORS_ALLOWED_ORIGINS` in backend `.env`
2. Restart Spring Boot application
3. Clear browser cache

### Issue: "Token expired"

**Solution:** The frontend automatically refreshes tokens. Check:
1. Refresh token is present in localStorage
2. Backend `/api/auth/zoho/refresh` endpoint is working

### Issue: "User info not loading"

**Solution:**
1. Check scope `AaaServer.profile.READ` is enabled in Zoho API Console
2. Regenerate tokens by logging out and logging in again

---

## 📚 API Endpoints Reference

### Backend (Spring Boot)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/zoho/token` | Exchange authorization code for access token |
| POST | `/api/auth/zoho/refresh` | Refresh access token using refresh token |
| GET | `/api/auth/zoho/userinfo` | Get authenticated user profile |

### Zoho OAuth Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `https://accounts.zoho.com/oauth/v2/auth` | Authorization endpoint |
| POST | `https://accounts.zoho.com/oauth/v2/token` | Token endpoint |
| GET | `https://accounts.zoho.com/oauth/user/info` | User info endpoint |

---

## 🔄 OAuth 2.0 Flow Diagram

```
┌─────────┐          ┌─────────────┐          ┌──────────┐
│ Browser │          │   Backend   │          │   Zoho   │
└────┬────┘          └──────┬──────┘          └────┬─────┘
     │                      │                      │
     │ 1. Click Login       │                      │
     │─────────────────────>│                      │
     │                      │                      │
     │ 2. Redirect to Zoho  │                      │
     │─────────────────────────────────────────────>│
     │                      │                      │
     │ 3. User Authorizes   │                      │
     │<─────────────────────────────────────────────│
     │                      │                      │
     │ 4. Callback with Code│                      │
     │─────────────────────>│                      │
     │                      │                      │
     │                      │ 5. Exchange Code     │
     │                      │─────────────────────>│
     │                      │                      │
     │                      │ 6. Return Tokens     │
     │                      │<─────────────────────│
     │                      │                      │
     │ 7. Return Tokens     │                      │
     │<─────────────────────│                      │
     │                      │                      │
     │ 8. Fetch User Info   │                      │
     │─────────────────────>│                      │
     │                      │                      │
     │                      │ 9. Get User Profile  │
     │                      │─────────────────────>│
     │                      │                      │
     │                      │ 10. User Data        │
     │                      │<─────────────────────│
     │                      │                      │
     │ 11. User Data + JWT  │                      │
     │<─────────────────────│                      │
     │                      │                      │
```

---

## ✅ Success!

Once completed, your ERD Tool application will:
- ✅ Use Zoho IAM for secure authentication
- ✅ Support single sign-on (SSO) across Zoho ecosystem
- ✅ Automatically refresh tokens
- ✅ Provide seamless user experience
- ✅ Store user sessions securely

---

## 📞 Support

For Zoho OAuth issues:
- 📖 [Zoho OAuth Documentation](https://www.zoho.com/accounts/protocol/oauth.html)
- 💬 [Zoho Developer Forums](https://help.zoho.com/portal/en/community/zoho-developer-community)
- 📧 Email: support@zohocorp.com

For application issues:
- Check logs in `/home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend/logs`
- Review browser console for frontend errors
