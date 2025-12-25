# 14 - Testing Strategy

## 🧪 Testing Overview

```
Testing Pyramid:
                    ▲
                   /▲\
                  / E \       E2E Tests (Cypress/Playwright)
                 /  2  \      - Full user flows
                /   E   \     - Real browser testing
               ──────────
              /          \
             / Integration \   Integration Tests
            /     Tests     \  - REST API tests
           /                 \ - WebSocket tests
          /                   \- Database tests
         ─────────────────────
        /                      \
       /      Unit Tests        \  Unit Tests
      /                          \ - Services
     /                            \- Mappers
    ────────────────────────────── - Utilities
```

---

## 📦 Test Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Test Dependencies -->
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
    
    <!-- Testcontainers for PostgreSQL -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- WebSocket Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Awaitility for async testing -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 🔧 Test Configuration

### Base Test Configuration

```java
package com.chartdb;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("chartdb_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Test Application Properties

```yaml
# src/test/resources/application-test.yml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  datasource:
    # Configured dynamically by Testcontainers

jwt:
  secret: test-secret-key-for-jwt-signing-must-be-256-bits
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000

logging:
  level:
    com.chartdb: DEBUG
    org.springframework.security: DEBUG
```

---

## 🧪 Unit Tests

### Service Unit Tests

```java
package com.chartdb.service;

import com.chartdb.dto.DiagramDTO;
import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.entity.Diagram;
import com.chartdb.entity.User;
import com.chartdb.mapper.DiagramMapper;
import com.chartdb.repository.DiagramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagramServiceTest {
    
    @Mock
    private DiagramRepository diagramRepository;
    
    @Mock
    private DiagramMapper diagramMapper;
    
    @InjectMocks
    private DiagramService diagramService;
    
    private User testUser;
    private Diagram testDiagram;
    private DiagramDTO testDiagramDTO;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .displayName("Test User")
            .build();
        
        testDiagram = Diagram.builder()
            .id(UUID.randomUUID())
            .name("Test Diagram")
            .databaseType("postgresql")
            .owner(testUser)
            .build();
        
        testDiagramDTO = DiagramDTO.builder()
            .id(testDiagram.getId())
            .name(testDiagram.getName())
            .databaseType(testDiagram.getDatabaseType())
            .ownerId(testUser.getId())
            .build();
    }
    
    @Nested
    @DisplayName("getDiagram()")
    class GetDiagramTests {
        
        @Test
        @DisplayName("should return diagram when exists")
        void shouldReturnDiagramWhenExists() {
            when(diagramRepository.findById(testDiagram.getId()))
                .thenReturn(Optional.of(testDiagram));
            when(diagramMapper.toDTO(testDiagram))
                .thenReturn(testDiagramDTO);
            
            DiagramDTO result = diagramService.getDiagram(testDiagram.getId());
            
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testDiagram.getId());
            assertThat(result.getName()).isEqualTo("Test Diagram");
        }
        
        @Test
        @DisplayName("should throw exception when diagram not found")
        void shouldThrowWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(diagramRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> diagramService.getDiagram(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Diagram not found");
        }
    }
    
    @Nested
    @DisplayName("createDiagram()")
    class CreateDiagramTests {
        
        @Test
        @DisplayName("should create diagram successfully")
        void shouldCreateDiagram() {
            CreateDiagramRequest request = new CreateDiagramRequest();
            request.setName("New Diagram");
            request.setDatabaseType("postgresql");
            
            when(diagramMapper.toEntity(request)).thenReturn(testDiagram);
            when(diagramRepository.save(any(Diagram.class))).thenReturn(testDiagram);
            when(diagramMapper.toDTO(testDiagram)).thenReturn(testDiagramDTO);
            
            DiagramDTO result = diagramService.createDiagram(request, testUser.getId());
            
            assertThat(result).isNotNull();
            verify(diagramRepository).save(any(Diagram.class));
        }
    }
}
```

### Mapper Unit Tests

```java
package com.chartdb.mapper;

import com.chartdb.dto.ColumnDTO;
import com.chartdb.entity.Column;
import com.chartdb.entity.Table;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnMapperTest {
    
    private final ColumnMapper mapper = Mappers.getMapper(ColumnMapper.class);
    
    @Test
    void shouldMapColumnToDTO() {
        Table table = Table.builder()
            .id(UUID.randomUUID())
            .name("users")
            .build();
        
        Column column = Column.builder()
            .id(UUID.randomUUID())
            .table(table)
            .name("id")
            .dataType("uuid")
            .isPrimaryKey(true)
            .isNullable(false)
            .build();
        
        ColumnDTO dto = mapper.toDTO(column);
        
        assertThat(dto.getId()).isEqualTo(column.getId());
        assertThat(dto.getTableId()).isEqualTo(table.getId());
        assertThat(dto.getName()).isEqualTo("id");
        assertThat(dto.getDataType()).isEqualTo("uuid");
        assertThat(dto.getIsPrimaryKey()).isTrue();
        assertThat(dto.getIsNullable()).isFalse();
    }
    
    @Test
    void shouldBuildDisplayTypeWithLength() {
        Column column = Column.builder()
            .dataType("varchar")
            .characterMaxLength(255)
            .build();
        
        String displayType = mapper.buildDisplayType(column);
        
        assertThat(displayType).isEqualTo("VARCHAR(255)");
    }
    
    @Test
    void shouldBuildDisplayTypeWithPrecisionAndScale() {
        Column column = Column.builder()
            .dataType("decimal")
            .numericPrecision(10)
            .numericScale(2)
            .build();
        
        String displayType = mapper.buildDisplayType(column);
        
        assertThat(displayType).isEqualTo("DECIMAL(10,2)");
    }
}
```

---

## 🔌 Integration Tests

### REST API Tests

```java
package com.chartdb.controller;

import com.chartdb.AbstractIntegrationTest;
import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.request.UserLoginRequest;
import com.chartdb.dto.request.UserRegistrationRequest;
import com.chartdb.dto.response.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class DiagramControllerIntegrationTest extends AbstractIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String accessToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // Register and login a test user
        UserRegistrationRequest regRequest = new UserRegistrationRequest();
        regRequest.setEmail("test" + System.currentTimeMillis() + "@example.com");
        regRequest.setPassword("password123");
        regRequest.setDisplayName("Test User");
        
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        AuthResponse authResponse = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            AuthResponse.class
        );
        
        accessToken = authResponse.getAccessToken();
    }
    
    @Test
    @DisplayName("POST /diagrams - should create diagram")
    void shouldCreateDiagram() throws Exception {
        CreateDiagramRequest request = new CreateDiagramRequest();
        request.setName("Test Database");
        request.setDatabaseType("postgresql");
        request.setDescription("A test diagram");
        
        mockMvc.perform(post("/diagrams")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Test Database"))
            .andExpect(jsonPath("$.databaseType").value("postgresql"));
    }
    
    @Test
    @DisplayName("GET /diagrams - should return user diagrams")
    void shouldReturnUserDiagrams() throws Exception {
        // First create a diagram
        CreateDiagramRequest request = new CreateDiagramRequest();
        request.setName("My Diagram");
        request.setDatabaseType("mysql");
        
        mockMvc.perform(post("/diagrams")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        
        // Then fetch diagrams
        mockMvc.perform(get("/diagrams")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.content[0].name").value("My Diagram"));
    }
    
    @Test
    @DisplayName("DELETE /diagrams/{id} - should delete diagram")
    void shouldDeleteDiagram() throws Exception {
        // Create a diagram
        CreateDiagramRequest request = new CreateDiagramRequest();
        request.setName("To Delete");
        request.setDatabaseType("sqlite");
        
        MvcResult createResult = mockMvc.perform(post("/diagrams")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        
        String diagramId = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        ).get("id").asText();
        
        // Delete the diagram
        mockMvc.perform(delete("/diagrams/" + diagramId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());
        
        // Verify it's deleted
        mockMvc.perform(get("/diagrams/" + diagramId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Unauthorized request should return 401")
    void shouldReturn401ForUnauthorized() throws Exception {
        mockMvc.perform(get("/diagrams"))
            .andExpect(status().isUnauthorized());
    }
}
```

### WebSocket Integration Tests

```java
package com.chartdb.websocket;

import com.chartdb.AbstractIntegrationTest;
import com.chartdb.websocket.message.CursorMoveMessage;
import com.chartdb.websocket.message.JoinDiagramMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WebSocketIntegrationTest extends AbstractIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private WebSocketStompClient stompClient;
    private String wsUrl;
    private String accessToken;
    
    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
        
        stompClient = new WebSocketStompClient(new SockJsClient(
            Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
        ));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        // Get access token (similar to REST test setup)
        // accessToken = ...
    }
    
    @Test
    @DisplayName("Should connect to WebSocket")
    void shouldConnectToWebSocket() throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        
        StompSession session = stompClient.connectAsync(
            wsUrl,
            new WebSocketHttpHeaders(),
            headers,
            new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    messages.add("connected");
                }
            }
        ).get(5, TimeUnit.SECONDS);
        
        assertThat(session.isConnected()).isTrue();
        assertThat(messages.poll(5, TimeUnit.SECONDS)).isEqualTo("connected");
        
        session.disconnect();
    }
    
    @Test
    @DisplayName("Should receive cursor updates")
    void shouldReceiveCursorUpdates() throws Exception {
        String diagramId = "test-diagram-id";
        BlockingQueue<CursorMoveMessage> cursorMessages = new LinkedBlockingQueue<>();
        
        // Connect first user
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        
        StompSession session1 = stompClient.connectAsync(
            wsUrl, new WebSocketHttpHeaders(), headers,
            new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
        
        // Subscribe to cursor updates
        session1.subscribe(
            "/topic/diagram/" + diagramId + "/cursor-update",
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return CursorMoveMessage.class;
                }
                
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    cursorMessages.add((CursorMoveMessage) payload);
                }
            }
        );
        
        // Send cursor movement
        CursorMoveMessage cursorMessage = CursorMoveMessage.builder()
            .diagramId(diagramId)
            .x(100.0)
            .y(200.0)
            .build();
        
        session1.send("/app/cursor-move", cursorMessage);
        
        // Verify message received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            CursorMoveMessage received = cursorMessages.poll();
            assertThat(received).isNotNull();
            assertThat(received.getX()).isEqualTo(100.0);
            assertThat(received.getY()).isEqualTo(200.0);
        });
        
        session1.disconnect();
    }
}
```

---

## 🔐 Security Tests

```java
package com.chartdb.security;

import com.chartdb.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SecurityTest extends AbstractIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Public endpoints should be accessible without auth")
    void publicEndpointsAccessible() throws Exception {
        mockMvc.perform(get("/auth/health"))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Protected endpoints require authentication")
    void protectedEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/diagrams"))
            .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/diagrams"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Expired token should be rejected")
    void expiredTokenRejected() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzUxMiJ9..."; // An expired token
        
        mockMvc.perform(get("/diagrams")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## 📊 Test Utilities

```java
package com.chartdb.test;

import com.chartdb.entity.*;
import java.util.UUID;

public class TestDataFactory {
    
    public static User createUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .email("test" + System.currentTimeMillis() + "@example.com")
            .displayName("Test User")
            .passwordHash("$2a$10$...")
            .provider("local")
            .emailVerified(true)
            .build();
    }
    
    public static Diagram createDiagram(User owner) {
        return Diagram.builder()
            .id(UUID.randomUUID())
            .name("Test Diagram")
            .databaseType("postgresql")
            .owner(owner)
            .isPublic(false)
            .build();
    }
    
    public static Table createTable(Diagram diagram, String name) {
        return Table.builder()
            .id(UUID.randomUUID())
            .diagram(diagram)
            .name(name)
            .positionX(100.0)
            .positionY(100.0)
            .color("#3B82F6")
            .build();
    }
    
    public static Column createColumn(Table table, String name, String type) {
        return Column.builder()
            .id(UUID.randomUUID())
            .table(table)
            .name(name)
            .dataType(type)
            .isPrimaryKey(false)
            .isNullable(true)
            .build();
    }
}
```

---

**← Previous:** `13-SECURITY-JWT.md` | **Next:** `15-DEPLOYMENT.md` →
