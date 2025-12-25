# 🚀 ERD Tool - Quick Start (5 Minutes)

## ⚡ Super Fast Setup

### 1️⃣ Get Zoho OAuth Credentials (2 min)

1. Go to: https://api-console.zoho.com/
2. Click **"Add Client"** → **"Server-based Applications"**
3. Fill in:
   ```
   Client Name: ERD Tool
   Homepage URL: http://localhost:3000
   Redirect URI: http://localhost:3000/auth/callback
   Scope: AaaServer.profile.READ
   ```
4. Click **"CREATE"**
5. Copy **Client ID** and **Client Secret**

---

### 2️⃣ Configure Backend (1 min)

<TERMINAL>
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend
cat > .env << 'EOF'
DATABASE_URL=jdbc:postgresql://localhost:5432/erdtool
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
ZOHO_CLIENT_ID=PASTE_YOUR_CLIENT_ID_HERE
ZOHO_CLIENT_SECRET=PASTE_YOUR_CLIENT_SECRET_HERE
ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
CORS_ALLOWED_ORIGINS=http://localhost:3000
FRONTEND_URL=http://localhost:3000
PORT=8080
EOF
</TERMINAL>

**Replace** `PASTE_YOUR_CLIENT_ID_HERE` and `PASTE_YOUR_CLIENT_SECRET_HERE` with values from step 1.

---

### 3️⃣ Configure Frontend (1 min)

<TERMINAL>
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend
cat > .env << 'EOF'
VITE_ZOHO_CLIENT_ID=PASTE_YOUR_CLIENT_ID_HERE
VITE_ZOHO_REDIRECT_URI=http://localhost:3000/auth/callback
VITE_ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
VITE_API_BASE_URL=/api
EOF
</TERMINAL>

**Replace** `PASTE_YOUR_CLIENT_ID_HERE` with the same Client ID from step 1.

---

### 4️⃣ Start Database (30 sec)

<TERMINAL>
sudo systemctl start postgresql
psql -U postgres -c "CREATE DATABASE IF NOT EXISTS erdtool;"
</TERMINAL>

---

### 5️⃣ Start Backend (30 sec)

**Terminal 1:**
<TERMINAL>
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-backend
mvn spring-boot:run
</TERMINAL>

Wait for: `Started ErdToolApplication in X.XXX seconds`

---

### 6️⃣ Start Frontend (30 sec)

**Terminal 2:**
<TERMINAL>
cd /home/workspace/PLAYBOOKS_DATASPACE/erdtool-frontend
npm install
npm run dev
</TERMINAL>

Wait for: `Local: http://localhost:3000/`

---

### 7️⃣ Test Login! 🎉

1. Open: **http://localhost:3000**
2. Click: **"Sign in with Zoho"**
3. Login with your Zoho credentials
4. Authorize the app
5. **You're logged in!** ✅

---

## 🎯 What You Have Now

- ✅ **Backend API** running on port 8080
- ✅ **Frontend UI** running on port 3000
- ✅ **Zoho IAM** authentication integrated
- ✅ **PostgreSQL** database connected
- ✅ **JWT tokens** with auto-refresh
- ✅ **Protected routes** working

---

## 📚 Full Documentation

- **Detailed Setup:** `SETUP_GUIDE.md`
- **Zoho Configuration:** `ZOHO_IAM_SETUP.md`
- **Implementation Details:** `IMPLEMENTATION_COMPLETE.md`
- **Frontend Docs:** `erdtool-frontend/README.md`

---

## 🆘 Issues?

### Backend won't start?
```bash
# Check PostgreSQL
sudo systemctl status postgresql

# Create database
psql -U postgres -c "CREATE DATABASE erdtool;"

# Check port
lsof -i:8080
```

### Frontend won't start?
```bash
# Install dependencies
cd erdtool-frontend
npm install

# Check port
lsof -i:3000
```

### Zoho OAuth error?
- Verify Client ID and Secret are correct
- Check redirect URI matches **exactly**: `http://localhost:3000/auth/callback`
- Clear browser cache and localStorage

---

## 🎨 Next Steps

1. **Customize the UI** - Edit `LoginPage.tsx`
2. **Add diagram editor** - Integrate canvas library
3. **Build API endpoints** - Add diagram operations
4. **Deploy to production** - See `SETUP_GUIDE.md`

---

## ✅ Success Checklist

- [ ] Zoho OAuth app created
- [ ] Backend `.env` configured
- [ ] Frontend `.env` configured
- [ ] PostgreSQL running
- [ ] Backend running (port 8080)
- [ ] Frontend running (port 3000)
- [ ] Can login with Zoho
- [ ] Tokens stored in localStorage
- [ ] API calls working

---

**Happy Coding!** 🚀

**Time to first login:** ~5 minutes ⏱️
