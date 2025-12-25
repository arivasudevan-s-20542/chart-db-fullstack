# 04 - Backend Structure & Setup

## 🏗️ Spring Boot Project Setup

### Step 1: Create Project Directory Structure
```bash
# Navigate to workspace
cd /home/workspace/PLAYBOOKS_DATASPACE

# Copy existing chartdb to new location
cp -r chartdb chartdb-fullstack/frontend

# Create backend directory
mkdir -p chartdb-fullstack/backend

# Navigate to backend
cd chartdb-fullstack/backend
```

### Step 2: Initialize Spring Boot Project

You can use Spring Initializr (https://start.spring.io) or create manually:

---

## 📦 pom.xml (Maven)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
        <relativePath/>
    </parent>
    
    <groupId>com.chartdb</groupId>
    <artifactId>chartdb-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>ChartDB Backend</name>
    <description>Real-time collaborative ERD tool backend</description>
    
    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.3</jjwt.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Flyway for Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        
        <!-- JWT Authentication -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- MapStruct for DTO Mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        
        <!-- Redis for Caching (Optional) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        
        <!-- Hibernate Types for JSONB -->
        <dependency>
            <groupId>io.hypersistence</groupId>
            <artifactId>hypersistence-utils-hibernate-63</artifactId>
            <version>3.7.0</version>
        </dependency>
        
        <!-- Development Tools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            
            <!-- Compiler with Lombok and MapStruct -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 📁 Complete Directory Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── chartdb/
│   │   │           ├── ChartDbApplication.java              # Main entry point
│   │   │           │
│   │   │           ├── config/                              # Configuration classes
│   │   │           │   ├── WebSocketConfig.java             # WebSocket + STOMP config
│   │   │           │   ├── SecurityConfig.java              # Spring Security config
│   │   │           │   ├── CorsConfig.java                  # CORS configuration
│   │   │           │   ├── JpaConfig.java                   # JPA/Hibernate config
│   │   │           │   ├── RedisConfig.java                 # Redis config (optional)
│   │   │           │   └── AsyncConfig.java                 # Async processing config
│   │   │           │
│   │   │           ├── controller/                          # REST Controllers
│   │   │           │   ├── AuthController.java              # Auth endpoints
│   │   │           │   ├── DiagramController.java           # Diagram CRUD
│   │   │           │   ├── TableController.java             # Table CRUD
│   │   │           │   ├── ColumnController.java            # Column CRUD
│   │   │           │   ├── RelationshipController.java      # Relationship CRUD
│   │   │           │   ├── UserController.java              # User profile
│   │   │           │   ├── CollaboratorController.java      # Presence/collaborators
│   │   │           │   └── ExportController.java            # SQL/DDL export
│   │   │           │
│   │   │           ├── websocket/                           # WebSocket handlers
│   │   │           │   ├── WebSocketController.java         # Message handlers
│   │   │           │   ├── WebSocketEventListener.java      # Connect/disconnect events
│   │   │           │   ├── WebSocketAuthInterceptor.java    # Auth for WebSocket
│   │   │           │   └── message/                         # WebSocket message types
│   │   │           │       ├── CursorMoveMessage.java
│   │   │           │       ├── TableMoveMessage.java
│   │   │           │       ├── TableCreateMessage.java
│   │   │           │       ├── TableUpdateMessage.java
│   │   │           │       ├── TableDeleteMessage.java
│   │   │           │       ├── ColumnMessage.java
│   │   │           │       ├── RelationshipMessage.java
│   │   │           │       ├── JoinDiagramMessage.java
│   │   │           │       ├── LeaveDiagramMessage.java
│   │   │           │       └── LockMessage.java
│   │   │           │
│   │   │           ├── service/                             # Business logic
│   │   │           │   ├── AuthService.java                 # Authentication logic
│   │   │           │   ├── UserService.java                 # User management
│   │   │           │   ├── DiagramService.java              # Diagram operations
│   │   │           │   ├── TableService.java                # Table operations
│   │   │           │   ├── ColumnService.java               # Column operations
│   │   │           │   ├── RelationshipService.java         # Relationship operations
│   │   │           │   ├── CollaborationService.java        # Real-time collab
│   │   │           │   ├── PermissionService.java           # Access control
│   │   │           │   ├── NotificationService.java         # WebSocket broadcasting
│   │   │           │   ├── LockService.java                 # Optimistic locking
│   │   │           │   ├── AuditService.java                # Audit logging
│   │   │           │   ├── VersionService.java              # Version history
│   │   │           │   └── ExportService.java               # SQL generation
│   │   │           │
│   │   │           ├── repository/                          # Data access
│   │   │           │   ├── UserRepository.java
│   │   │           │   ├── DiagramRepository.java
│   │   │           │   ├── TableRepository.java
│   │   │           │   ├── ColumnRepository.java
│   │   │           │   ├── RelationshipRepository.java
│   │   │           │   ├── DiagramPermissionRepository.java
│   │   │           │   ├── ActiveCollaboratorRepository.java
│   │   │           │   ├── DiagramVersionRepository.java
│   │   │           │   ├── AuditLogRepository.java
│   │   │           │   └── TableLockRepository.java
│   │   │           │
│   │   │           ├── model/                               # JPA Entities
│   │   │           │   ├── User.java
│   │   │           │   ├── Diagram.java
│   │   │           │   ├── Table.java
│   │   │           │   ├── Column.java
│   │   │           │   ├── Relationship.java
│   │   │           │   ├── DiagramPermission.java
│   │   │           │   ├── ActiveCollaborator.java
│   │   │           │   ├── DiagramVersion.java
│   │   │           │   ├── AuditLog.java
│   │   │           │   ├── TableLock.java
│   │   │           │   └── enums/                           # Enum types
│   │   │           │       ├── PermissionLevel.java
│   │   │           │       ├── RelationshipType.java
│   │   │           │       ├── DiagramStatus.java
│   │   │           │       └── ActionType.java
│   │   │           │
│   │   │           ├── dto/                                 # Data Transfer Objects
│   │   │           │   ├── request/                         # Request DTOs
│   │   │           │   │   ├── LoginRequest.java
│   │   │           │   │   ├── RegisterRequest.java
│   │   │           │   │   ├── CreateDiagramRequest.java
│   │   │           │   │   ├── UpdateDiagramRequest.java
│   │   │           │   │   ├── CreateTableRequest.java
│   │   │           │   │   ├── UpdateTableRequest.java
│   │   │           │   │   ├── UpdatePositionRequest.java
│   │   │           │   │   ├── CreateColumnRequest.java
│   │   │           │   │   ├── UpdateColumnRequest.java
│   │   │           │   │   ├── CreateRelationshipRequest.java
│   │   │           │   │   └── ShareDiagramRequest.java
│   │   │           │   │
│   │   │           │   ├── response/                        # Response DTOs
│   │   │           │   │   ├── AuthResponse.java
│   │   │           │   │   ├── UserResponse.java
│   │   │           │   │   ├── DiagramResponse.java
│   │   │           │   │   ├── DiagramFullResponse.java
│   │   │           │   │   ├── TableResponse.java
│   │   │           │   │   ├── ColumnResponse.java
│   │   │           │   │   ├── RelationshipResponse.java
│   │   │           │   │   ├── CollaboratorResponse.java
│   │   │           │   │   └── ErrorResponse.java
│   │   │           │   │
│   │   │           │   └── mapper/                          # MapStruct mappers
│   │   │           │       ├── UserMapper.java
│   │   │           │       ├── DiagramMapper.java
│   │   │           │       ├── TableMapper.java
│   │   │           │       ├── ColumnMapper.java
│   │   │           │       └── RelationshipMapper.java
│   │   │           │
│   │   │           ├── security/                            # Security components
│   │   │           │   ├── JwtTokenProvider.java            # JWT generation/validation
│   │   │           │   ├── JwtAuthenticationFilter.java     # Request filter
│   │   │           │   ├── JwtAuthenticationEntryPoint.java # Auth error handler
│   │   │           │   ├── UserDetailsServiceImpl.java      # User details loader
│   │   │           │   ├── UserPrincipal.java               # Security principal
│   │   │           │   └── CurrentUser.java                 # @CurrentUser annotation
│   │   │           │
│   │   │           ├── exception/                           # Exception handling
│   │   │           │   ├── GlobalExceptionHandler.java      # @ControllerAdvice
│   │   │           │   ├── ResourceNotFoundException.java
│   │   │           │   ├── BadRequestException.java
│   │   │           │   ├── UnauthorizedException.java
│   │   │           │   ├── ForbiddenException.java
│   │   │           │   ├── ConflictException.java
│   │   │           │   └── LockedException.java
│   │   │           │
│   │   │           └── util/                                # Utilities
│   │   │               ├── IdGenerator.java                 # UUID generation
│   │   │               ├── ColorGenerator.java              # Random colors
│   │   │               └── SqlExporter.java                 # SQL generation
│   │   │
│   │   └── resources/
│   │       ├── application.yml                              # Main config
│   │       ├── application-dev.yml                          # Dev config
│   │       ├── application-prod.yml                         # Production config
│   │       ├── application-test.yml                         # Test config
│   │       │
│   │       └── db/
│   │           └── migration/                               # Flyway migrations
│   │               ├── V1__create_users_table.sql
│   │               ├── V2__create_diagrams_table.sql
│   │               ├── V3__create_tables_table.sql
│   │               ├── V4__create_columns_table.sql
│   │               ├── V5__create_relationships_table.sql
│   │               ├── V6__create_permissions_table.sql
│   │               ├── V7__create_collaborators_table.sql
│   │               ├── V8__create_versions_table.sql
│   │               ├── V9__create_audit_logs_table.sql
│   │               ├── V10__create_table_locks_table.sql
│   │               └── V11__create_indexes.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── chartdb/
│                   ├── ChartDbApplicationTests.java
│                   ├── controller/
│                   │   ├── AuthControllerTest.java
│                   │   ├── DiagramControllerTest.java
│                   │   └── TableControllerTest.java
│                   ├── service/
│                   │   ├── DiagramServiceTest.java
│                   │   └── TableServiceTest.java
│                   └── integration/
│                       ├── WebSocketIntegrationTest.java
│                       └── CollaborationIntegrationTest.java
│
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## ⚙️ application.yml

```yaml
# Main application configuration
spring:
  application:
    name: chartdb-backend
  
  profiles:
    active: dev
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema management
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
    open-in-view: false
  
  # Jackson Configuration
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null
    time-zone: UTC
  
  # Flyway Configuration
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api
  error:
    include-message: always
    include-binding-errors: always

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-min-32-chars}
  access-token-expiration: 900000      # 15 minutes
  refresh-token-expiration: 604800000  # 7 days

# CORS Configuration
cors:
  allowed-origins: 
    - http://localhost:5173
    - http://localhost:3000
  allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
  max-age: 3600

# WebSocket Configuration
websocket:
  allowed-origins:
    - http://localhost:5173
    - http://localhost:3000
  endpoint: /ws
  application-prefix: /app
  broker-prefix: /topic

# Logging Configuration
logging:
  level:
    root: INFO
    com.chartdb: DEBUG
    org.springframework.web: INFO
    org.springframework.websocket: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

---

## ⚙️ application-dev.yml

```yaml
# Development environment configuration
spring:
  # PostgreSQL Development Database
  datasource:
    url: jdbc:postgresql://localhost:5432/chartdb_dev
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
  
  # JPA Development Settings
  jpa:
    hibernate:
      ddl-auto: update  # Allow schema updates in dev
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Redis Development (optional)
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000

# Development JWT (use secure key in production!)
jwt:
  secret: dev-secret-key-for-testing-only-change-in-production-32chars

# Development CORS (allow all in dev)
cors:
  allowed-origins:
    - http://localhost:5173
    - http://localhost:3000
    - http://127.0.0.1:5173
    - http://127.0.0.1:3000

# Debug Logging
logging:
  level:
    com.chartdb: DEBUG
    org.springframework.security: DEBUG
    org.springframework.websocket: DEBUG
```

---

## ⚙️ application-prod.yml

```yaml
# Production environment configuration
spring:
  # PostgreSQL Production Database
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
  
  # JPA Production Settings
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-update in production
    show-sql: false
    properties:
      hibernate:
        generate_statistics: false
  
  # Redis Production
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000

# Production JWT (from environment)
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:900000}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

# Production CORS
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:https://chartdb.io}

# Production Logging
logging:
  level:
    root: WARN
    com.chartdb: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN

# Production Server
server:
  tomcat:
    max-threads: 200
    accept-count: 100
```

---

## 🚀 Main Application Class

```java
package com.chartdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ChartDbApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ChartDbApplication.class, args);
    }
}
```

---

## 🐳 docker-compose.yml (Development)

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: chartdb-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: chartdb_dev
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache (Optional)
  redis:
    image: redis:7-alpine
    container_name: chartdb-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # pgAdmin (Optional - for DB management)
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: chartdb-pgadmin
    restart: unless-stopped
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@chartdb.io
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
  redis_data:
```

---

## 🔧 Running the Backend

```bash
# 1. Start dependencies (PostgreSQL, Redis)
docker-compose up -d postgres redis

# 2. Wait for PostgreSQL to be ready
docker-compose logs -f postgres

# 3. Run Spring Boot application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/chartdb-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## ✅ Verification Checklist

After setup, verify:

- [ ] PostgreSQL is running and accessible on port 5432
- [ ] Database `chartdb_dev` exists
- [ ] Spring Boot starts without errors
- [ ] Flyway migrations run successfully
- [ ] API is accessible at http://localhost:8080/api
- [ ] WebSocket endpoint available at http://localhost:8080/api/ws
- [ ] Health check returns OK: http://localhost:8080/api/actuator/health

---

**← Previous:** `03-DATABASE-SCHEMA.md` | **Next:** `05-BACKEND-ENTITIES.md` →
