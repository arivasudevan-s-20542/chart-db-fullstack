# Jenkins CI/CD Setup Guide

This guide explains how to set up Jenkins for the ChartDB project.

## Prerequisites

- Jenkins server (2.400+) - **Installed on the same server as the application**
- Docker installed on Jenkins server
- Git plugin
- Docker Pipeline plugin
- Jenkins user added to docker group

## Setup Jenkins User

```bash
# Add jenkins user to docker group
sudo usermod -aG docker jenkins

# Restart Jenkins
sudo systemctl restart jenkins
```

## Required Jenkins Credentials

Create the following credentials in Jenkins (Manage Jenkins → Credentials):

### 1. GitHub Packages Token
- **ID**: `github-packages-token`
- **Type**: Username with password
- **Username**: Your GitHub username
- **Password**: GitHub Personal Access Token with `read:packages` and `write:packages` scopes

### 2. GHCR Token (same as above, different ID)
- **ID**: `ghcr-token`
- **Type**: Secret text
- **Value**: GitHub token for container registry access

### 5. Database Credentials
- **ID**: `postgres-password`
- **Type**: Secret text
- **Value**: PostgreSQL password

### 6. JWT Secret
- **ID**: `jwt-secret`
- **Type**: Secret text
- **Value**: JWT secret key (256-bit minimum)

### 7. OAuth Credentials

#### Google OAuth
- **ID**: `google-client-id`
- **Type**: Secret text
- **Value**: Google OAuth Client ID

- **ID**: `google-client-secret`
- **Type**: Secret text
- **Value**: Google OAuth Client Secret

#### GitHub OAuth
- **ID**: `github-oauth-client-id`
- **Type**: Secret text
- **Value**: GitHub OAuth Client ID

- **ID**: `github-oauth-client-secret`
- **Type**: Secret text
- **Value**: GitHub OAuth Client Secret

#### Zoho OAuth
- **ID**: `zoho-client-id`
- **Type**: Secret text
- **Value**: Zoho OAuth Client ID

- **ID**: `zoho-client-secret`
- **Type**: Secret text
- **Value**: Zoho OAuth Client Secret

## Pipeline Configuration

### Create a New Pipeline Job

1. Go to Jenkins Dashboard → New Item
2. Enter job name (e.g., `chartdb-pipeline`)
3. Select "Pipeline" and click OK

### Configure Pipeline

1. **General**:
   - ✅ GitHub project: `https://github.com/arivasudevan-s-20542/chart-db-fullstack`
   - ✅ Build after other projects are built (optional)

2. **Build Triggers**:
   - ✅ GitHub hook trigger for GITScm polling
   - ✅ Poll SCM: `H/5 * * * *` (every 5 minutes as fallback)

3. **Pipeline**:
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: `https://github.com/arivasudevan-s-20542/chart-db-fullstack.git`
   - Credentials: Select your GitHub credentials
   - Branches to build: `*/main` or `*/feature/*` or `**` (for all branches)
   - Script Path: `Jenkinsfile`

### GitHub Webhook (Optional but Recommended)

1. Go to your GitHub repository → Settings → Webhooks
2. Click "Add webhook"
3. Payload URL: `http://your-jenkins-server/github-webhook/`
4. Content type: `application/json`
5. Events: "Just the push event"
6. ✅ Active

## Environment Variables

You can override these in Jenkins job configuration:

- `VITE_API_URL`: Frontend API URL (default: `/api`)
- `VITE_WS_URL`: Frontend WebSocket URL (default: `/ws`)

## Pipeline Stages

The Jenkinsfile includes:

1. **Checkout**: Clone repository and get commit info
2. **Build Backend**: Maven build with Java 21
3. **Test Backend**: Run JUnit tests
4. **Build Frontend**: npm build with Vite
5. **Test Frontend**: Run tests and linting
6. **Build & Push Docker Images**: Build and push to GHCR
7. **Deploy to Server**: SSH deploy to production

## Branch-based Deployment

- **main**: Deploys with `latest` tag to production
- **develop**: Builds but doesn't deploy
- **feature/***: Deploys with `feature-latest` tag for testing

## Troubleshooting

### Docker Build Fails
- Ensure Jenkins user has Docker permissions: `sudo usermod -aG docker jenkins`
- Restart Jenkins: `sudo systemctl restart jenkins`
- Verify: `sudo -u jenkins docker ps`

### Deployment Directory Permission Issues
- Ensure Jenkins can write to ~/chartdb: `sudo mkdir -p /var/lib/jenkins/chartdb`
- Or use Jenkins home: Deployment goes to `~/chartdb` relative to Jenkins user home

### Maven/NPM Issues
- Ensure Maven and Node.js are installed on Jenkins server
- Configure tools in Jenkins: Manage Jenkins → Global Tool Configuration

### Docker Registry Authentication
- Verify GHCR token has correct permissions
- Test login: `echo "$TOKEN" | docker login ghcr.io -u username --password-stdin`

## Manual Trigger

To manually trigger the pipeline:
1. Go to your pipeline job
2. Click "Build Now"
3. Check "Console Output" for logs

## Comparison with GitHub Actions

| Feature | GitHub Actions | Jenkins |
|---------|---------------|---------|
| Hosted | ✅ GitHub-hosted | ❌ Self-hosted |
| Cost | Free tier available | Infrastructure cost |
| Setup | Easy (YAML in repo) | Requires server setup |
| Flexibility | Limited to GitHub ecosystem | Highly customizable |
| Integration | Native GitHub integration | Requires webhooks |
| Secrets | GitHub Secrets | Jenkins Credentials |

## Best Practices

1. **Keep credentials secure**: Never commit credentials to repository
2. **Use branch protection**: Require status checks before merging
3. **Monitor build times**: Optimize slow stages
4. **Regular backups**: Backup Jenkins configuration regularly
5. **Update plugins**: Keep Jenkins and plugins up to date

## Support

For issues or questions:
- Check Jenkins logs: `/var/log/jenkins/jenkins.log`
- Review pipeline console output
- Contact DevOps team
