# OAuth2 Social Login Setup Guide

ChartDB supports social login with Google, GitHub, and Zoho. This guide explains how to configure each provider.

## Prerequisites

1. A running ChartDB backend instance
2. Developer accounts with the OAuth providers you want to enable

## Environment Variables

Add the following environment variables to your backend:

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# GitHub OAuth2
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# Zoho OAuth2
ZOHO_CLIENT_ID=your-zoho-client-id
ZOHO_CLIENT_SECRET=your-zoho-client-secret

# Frontend redirect URI (after successful OAuth)
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/callback
```

---

## Google OAuth2 Setup

### 1. Go to Google Cloud Console
- Visit [Google Cloud Console](https://console.cloud.google.com/)
- Create a new project or select an existing one

### 2. Enable OAuth2 API
- Go to **APIs & Services** → **OAuth consent screen**
- Configure the consent screen (External or Internal based on your needs)
- Add scopes: `email`, `profile`

### 3. Create Credentials
- Go to **APIs & Services** → **Credentials**
- Click **Create Credentials** → **OAuth client ID**
- Application type: **Web application**
- Name: `ChartDB`
- Authorized JavaScript origins:
  - `http://localhost:8080` (development)
  - `https://your-domain.com` (production)
- Authorized redirect URIs:
  - `http://localhost:8080/login/oauth2/code/google`
  - `https://your-domain.com/login/oauth2/code/google`

### 4. Copy Credentials
- Copy the **Client ID** and **Client Secret**
- Set as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`

---

## GitHub OAuth2 Setup

### 1. Go to GitHub Developer Settings
- Visit [GitHub Developer Settings](https://github.com/settings/developers)
- Click **New OAuth App**

### 2. Configure the Application
- **Application name**: `ChartDB`
- **Homepage URL**: `http://localhost:5173` (or your production URL)
- **Authorization callback URL**: 
  - `http://localhost:8080/login/oauth2/code/github` (development)
  - `https://your-domain.com/login/oauth2/code/github` (production)

### 3. Copy Credentials
- Copy the **Client ID**
- Generate a **Client Secret**
- Set as `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`

### Note on Email Privacy
- If GitHub users have private emails, they need to make their email public OR
- Use the GitHub API to fetch the primary email (requires `user:email` scope)

---

## Zoho OAuth2 Setup

### 1. Go to Zoho API Console
- Visit [Zoho API Console](https://api-console.zoho.com/)
- Click **Add Client** → **Server-based Applications**

### 2. Configure the Application
- **Client Name**: `ChartDB`
- **Homepage URL**: `http://localhost:5173`
- **Authorized Redirect URIs**:
  - `http://localhost:8080/login/oauth2/code/zoho`
  - `https://your-domain.com/login/oauth2/code/zoho`

### 3. Select Scopes
- `AaaServer.profile.READ` - For reading user profile
- `ZohoContacts.userphoto.READ` - For reading user photo (optional)

### 4. Copy Credentials
- Copy the **Client ID** and **Client Secret**
- Set as `ZOHO_CLIENT_ID` and `ZOHO_CLIENT_SECRET`

### Zoho Regional Endpoints
If your Zoho account is in a specific region, you may need to modify the provider URLs in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          zoho:
            # For EU region
            authorization-uri: https://accounts.zoho.eu/oauth/v2/auth
            token-uri: https://accounts.zoho.eu/oauth/v2/token
            user-info-uri: https://accounts.zoho.eu/oauth/user/info
            # For IN region
            # authorization-uri: https://accounts.zoho.in/oauth/v2/auth
            # token-uri: https://accounts.zoho.in/oauth/v2/token
            # user-info-uri: https://accounts.zoho.in/oauth/user/info
```

---

## Production Configuration

For production, update the following:

### 1. Update Redirect URIs
In `application.yml` or via environment variable:
```yaml
app:
  oauth2:
    redirect-uri: https://your-frontend-domain.com/oauth2/callback
```

### 2. Update CORS Settings
Ensure your production frontend URL is in the allowed origins.

### 3. Update OAuth Provider Redirect URIs
Update all OAuth providers with your production callback URLs:
- `https://your-backend-domain.com/login/oauth2/code/google`
- `https://your-backend-domain.com/login/oauth2/code/github`
- `https://your-backend-domain.com/login/oauth2/code/zoho`

---

## How It Works

1. User clicks a social login button on the frontend
2. Frontend redirects to backend OAuth2 authorization endpoint: `/oauth2/authorize/{provider}`
3. Backend redirects to the OAuth provider's login page
4. User authenticates with the provider
5. Provider redirects back to backend: `/login/oauth2/code/{provider}`
6. Backend:
   - Validates the OAuth response
   - Creates or updates the user in the database
   - Generates JWT access and refresh tokens
   - Redirects to frontend with tokens: `/oauth2/callback?access_token=...&refresh_token=...`
7. Frontend stores the tokens and redirects to the main app

---

## Troubleshooting

### "Email not found from OAuth2 provider"
- GitHub: User needs to make their email public in GitHub settings
- Zoho: Ensure the `AaaServer.profile.READ` scope is enabled

### "redirect_uri_mismatch"
- Ensure the redirect URI in your OAuth provider settings exactly matches:
  `http(s)://your-backend/login/oauth2/code/{provider}`

### "invalid_client"
- Double-check your client ID and client secret
- Ensure no extra whitespace in environment variables

### OAuth works locally but not in production
- Update all redirect URIs in OAuth provider settings
- Ensure `OAUTH2_REDIRECT_URI` points to your production frontend
- Check CORS configuration includes your production domain
