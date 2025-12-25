# 15 - Deployment & DevOps

## 🐳 Docker Configuration

### Backend Dockerfile

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests -B

# Production stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy source
COPY . .

# Build args for environment
ARG VITE_API_URL
ARG VITE_WS_URL

ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL

# Build the application
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files
COPY --from=builder /app/dist /usr/share/nginx/html

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### Nginx Configuration

```nginx
# frontend/nginx.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    # Health check endpoint
    location /health {
        access_log off;
        return 200 'healthy';
        add_header Content-Type text/plain;
    }

    # API proxy
    location /api {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # WebSocket proxy
    location /ws {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400;
    }

    # SPA routing - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 🐙 Docker Compose

### Development

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: chartdb-postgres
    environment:
      POSTGRES_DB: chartdb
      POSTGRES_USER: chartdb
      POSTGRES_PASSWORD: ${DB_PASSWORD:-chartdb123}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U chartdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: chartdb-backend
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/chartdb
      SPRING_DATASOURCE_USERNAME: chartdb
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-chartdb123}
      JWT_SECRET: ${JWT_SECRET:-your-256-bit-secret-key-for-jwt-signing-min-32-chars}
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_API_URL: http://localhost:8080/api
        VITE_WS_URL: http://localhost:8080/ws
    container_name: chartdb-frontend
    depends_on:
      - backend
    ports:
      - "3000:80"

volumes:
  postgres_data:
```

### Production

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: chartdb-postgres
    restart: always
    environment:
      POSTGRES_DB: chartdb
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - backend-network
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G

  redis:
    image: redis:7-alpine
    container_name: chartdb-redis
    restart: always
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - backend-network
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

  backend:
    image: ${DOCKER_REGISTRY}/chartdb-backend:${VERSION:-latest}
    container_name: chartdb-backend
    restart: always
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/chartdb
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET}
      CORS_ALLOWED_ORIGINS: ${CORS_ORIGINS}
    networks:
      - backend-network
      - frontend-network
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '2'
          memory: 2G
      update_config:
        parallelism: 1
        delay: 10s

  frontend:
    image: ${DOCKER_REGISTRY}/chartdb-frontend:${VERSION:-latest}
    container_name: chartdb-frontend
    restart: always
    depends_on:
      - backend
    networks:
      - frontend-network
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '0.5'
          memory: 256M

  nginx:
    image: nginx:alpine
    container_name: chartdb-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/certs:/etc/nginx/certs
    depends_on:
      - frontend
      - backend
    networks:
      - frontend-network

networks:
  backend-network:
    driver: bridge
  frontend-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
```

---

## 🔄 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test-backend:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: chartdb_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Run tests
        run: ./mvnw verify -B
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/chartdb_test
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: target/site/jacoco/jacoco.xml

  test-frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run linter
        run: npm run lint
      
      - name: Run tests
        run: npm test -- --coverage
      
      - name: Build
        run: npm run build

  build-and-push:
    needs: [test-backend, test-frontend]
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    permissions:
      contents: read
      packages: write
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Build and push backend
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-backend:${{ github.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-backend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
      
      - name: Build and push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-frontend:${{ github.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-frontend:latest
          build-args: |
            VITE_API_URL=${{ secrets.PROD_API_URL }}
            VITE_WS_URL=${{ secrets.PROD_WS_URL }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    steps:
      - name: Deploy to production
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/chartdb
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --remove-orphans
            docker system prune -f
```

---

## ☸️ Kubernetes Deployment (Optional)

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chartdb-backend
  labels:
    app: chartdb-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: chartdb-backend
  template:
    metadata:
      labels:
        app: chartdb-backend
    spec:
      containers:
        - name: backend
          image: ghcr.io/yourorg/chartdb-backend:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: chartdb-secrets
                  key: database-url
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: chartdb-secrets
                  key: jwt-secret
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: chartdb-backend
spec:
  selector:
    app: chartdb-backend
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: chartdb-ingress
  annotations:
    nginx.ingress.kubernetes.io/websocket-services: "chartdb-backend"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
spec:
  rules:
    - host: chartdb.example.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: chartdb-backend
                port:
                  number: 80
          - path: /ws
            pathType: Prefix
            backend:
              service:
                name: chartdb-backend
                port:
                  number: 80
          - path: /
            pathType: Prefix
            backend:
              service:
                name: chartdb-frontend
                port:
                  number: 80
```

---

## 📊 Monitoring & Logging

### Application Properties for Production

```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.chartdb: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/chartdb/application.log
```

### Prometheus Scrape Config

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'chartdb-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

---

## 🔐 Environment Variables Reference

```bash
# .env.example

# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=chartdb
DB_USER=chartdb
DB_PASSWORD=your-secure-password

# JWT
JWT_SECRET=your-256-bit-secret-key-for-jwt-signing-minimum-32-characters

# Application
SPRING_PROFILES_ACTIVE=production
CORS_ORIGINS=https://chartdb.example.com

# Frontend
VITE_API_URL=https://api.chartdb.example.com
VITE_WS_URL=wss://api.chartdb.example.com/ws

# Docker Registry
DOCKER_REGISTRY=ghcr.io/yourorg
VERSION=latest

# Optional: OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
```

---

**← Previous:** `14-TESTING.md` | **Next:** `16-IMPLEMENTATION-CHECKLIST.md` →
