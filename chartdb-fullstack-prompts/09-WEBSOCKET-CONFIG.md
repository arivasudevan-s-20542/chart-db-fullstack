# 09 - WebSocket Configuration

## 🔌 WebSocket + STOMP Setup

### 1. WebSocket Configuration

```java
package com.chartdb.config;

import com.chartdb.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Value("${websocket.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // For production, consider using RabbitMQ or ActiveMQ
        config.enableSimpleBroker(
            "/topic",    // For broadcasting to all subscribers
            "/queue"     // For point-to-point messaging
        );
        
        // Prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary WebSocket endpoint
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins)
            .withSockJS()  // Fallback for browsers without WebSocket support
            .setHeartbeatTime(25000);  // Heartbeat every 25 seconds
        
        // Raw WebSocket endpoint (without SockJS)
        registry.addEndpoint("/ws-raw")
            .setAllowedOriginPatterns(allowedOrigins);
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication interceptor
        registration.interceptors(webSocketAuthInterceptor);
    }
}
```

---

### 2. WebSocket Authentication Interceptor

```java
package com.chartdb.websocket;

import com.chartdb.security.JwtTokenProvider;
import com.chartdb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Get authorization header
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    
                    try {
                        if (jwtTokenProvider.validateToken(token)) {
                            UserPrincipal userPrincipal = jwtTokenProvider.getUserPrincipalFromToken(token);
                            
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                    userPrincipal, 
                                    null, 
                                    userPrincipal.getAuthorities()
                                );
                            
                            accessor.setUser(authentication);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("WebSocket authenticated for user: {}", userPrincipal.getId());
                        }
                    } catch (Exception e) {
                        log.error("WebSocket authentication failed", e);
                        throw new RuntimeException("Authentication failed");
                    }
                }
            }
            
            // Also check query parameter (for SockJS)
            String query = (String) accessor.getSessionAttributes().get("token");
            if (query != null) {
                try {
                    if (jwtTokenProvider.validateToken(query)) {
                        UserPrincipal userPrincipal = jwtTokenProvider.getUserPrincipalFromToken(query);
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                userPrincipal, null, userPrincipal.getAuthorities());
                        accessor.setUser(authentication);
                    }
                } catch (Exception e) {
                    log.error("WebSocket token authentication failed", e);
                }
            }
        }
        
        return message;
    }
}
```

---

### 3. WebSocket Handshake Interceptor (for token in query params)

```java
package com.chartdb.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // Get token from query parameter
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null) {
                attributes.put("token", token);
            }
            
            // Get session ID for tracking
            String sessionId = servletRequest.getServletRequest().getSession().getId();
            attributes.put("sessionId", sessionId);
        }
        
        return true;
    }
    
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op
    }
}
```

---

### 4. WebSocket Event Listener

```java
package com.chartdb.websocket;

import com.chartdb.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final CollaborationService collaborationService;
    
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket connected: sessionId={}", sessionId);
    }
    
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        // Clean up collaborator session
        collaborationService.leaveByWebsocketSession(sessionId);
        
        log.info("WebSocket disconnected: sessionId={}", sessionId);
    }
    
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        
        log.debug("WebSocket subscribe: sessionId={}, destination={}", sessionId, destination);
    }
    
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        log.debug("WebSocket unsubscribe: sessionId={}", sessionId);
    }
}
```

---

### 5. Security Configuration for WebSocket

```java
package com.chartdb.config;

import com.chartdb.security.JwtAuthenticationEntryPoint;
import com.chartdb.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})  // Use CorsConfig
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()  // WebSocket endpoints
                .requestMatchers("/ws-raw/**").permitAll()
                .requestMatchers("/diagrams/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Protected endpoints
                .anyRequest().authenticated()
            );
        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

---

### 6. CORS Configuration

```java
package com.chartdb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Refresh-Token",
            "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 📨 WebSocket Message Types

### Message Classes

```java
// CursorMoveMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CursorMoveMessage {
    private String diagramId;
    private String userId;
    private String userName;
    private double x;
    private double y;
    private String color;
    private long timestamp;
}

// TableMoveMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableMoveMessage {
    private String diagramId;
    private String tableId;
    private double x;
    private double y;
    private String userId;
    private long timestamp;
}

// TableCreateMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableCreateMessage {
    private String diagramId;
    private String tableId;
    private String tableName;
    private double positionX;
    private double positionY;
    private String color;
    private String userId;
    private long timestamp;
}

// TableUpdateMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableUpdateMessage {
    private String diagramId;
    private String tableId;
    private String tableName;
    private String color;
    private Boolean isCollapsed;
    private String userId;
    private long timestamp;
}

// TableDeleteMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableDeleteMessage {
    private String diagramId;
    private String tableId;
    private String userId;
    private long timestamp;
}

// ColumnMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColumnMessage {
    private String diagramId;
    private String tableId;
    private String columnId;
    private String columnName;
    private String dataType;
    private String action;  // created, updated, deleted
    private String userId;
    private long timestamp;
}

// JoinDiagramMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinDiagramMessage {
    private String diagramId;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String cursorColor;
    private long timestamp;
}

// LeaveDiagramMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveDiagramMessage {
    private String diagramId;
    private String userId;
    private long timestamp;
}

// LockMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LockMessage {
    private String diagramId;
    private String tableId;
    private String userId;
    private String userName;
    private String action;  // locked, unlocked
    private long timestamp;
}

// SelectionMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SelectionMessage {
    private String diagramId;
    private String userId;
    private String userName;
    private String selectedTableId;
    private String selectedColumnId;
    private long timestamp;
}
```

---

## 🔄 WebSocket Topics Structure

```
WebSocket Topics and Destinations:

SUBSCRIBE (Client → Server):
──────────────────────────────────────────────────────────────
/topic/diagram/{diagramId}/table-created      # New table added
/topic/diagram/{diagramId}/table-updated      # Table properties changed
/topic/diagram/{diagramId}/table-moved        # Table position changed
/topic/diagram/{diagramId}/table-deleted      # Table removed
/topic/diagram/{diagramId}/column-created     # New column added
/topic/diagram/{diagramId}/column-updated     # Column changed
/topic/diagram/{diagramId}/column-deleted     # Column removed
/topic/diagram/{diagramId}/relationship-*     # Relationship changes
/topic/diagram/{diagramId}/cursor-update      # User cursor moved
/topic/diagram/{diagramId}/user-joined        # User joined diagram
/topic/diagram/{diagramId}/user-left          # User left diagram
/topic/diagram/{diagramId}/table-locked       # Table locked by user
/topic/diagram/{diagramId}/table-unlocked     # Table unlocked
/topic/diagram/{diagramId}/selection-changed  # User selection changed

SEND (Client → Server):
──────────────────────────────────────────────────────────────
/app/join-diagram                             # Join a diagram
/app/leave-diagram                            # Leave a diagram
/app/cursor-move                              # Report cursor position
/app/table-move                               # Table being dragged
/app/lock-table                               # Request table lock
/app/unlock-table                             # Release table lock
/app/select                                   # Update selection

DIRECT (Server → User):
──────────────────────────────────────────────────────────────
/user/{userId}/queue/errors                   # Error messages to specific user
/user/{userId}/queue/notifications            # Personal notifications
```

---

## 📋 WebSocket Message Flow Examples

### Example 1: User Joins Diagram
```
1. Client connects:       WebSocket CONNECT /ws
2. Client subscribes:     SUBSCRIBE /topic/diagram/{id}/*
3. Client sends:          SEND /app/join-diagram { diagramId }
4. Server processes:      CollaborationService.joinDiagram()
5. Server broadcasts:     SEND /topic/diagram/{id}/user-joined { user info }
6. Other clients:         Receive user-joined, add to collaborator list
```

### Example 2: Table Drag
```
1. User starts drag:      (No WebSocket - local only)
2. During drag (50ms):    SEND /app/table-move { tableId, x, y }
3. Server broadcasts:     SEND /topic/diagram/{id}/table-moved { tableId, x, y, userId }
4. Other clients:         Receive table-moved, update position (if userId != self)
5. Drag ends:             REST PUT /tables/{id}/position (persists final position)
```

### Example 3: Cursor Tracking
```
1. Mouse move (throttled): SEND /app/cursor-move { diagramId, x, y }
2. Server broadcasts:      SEND /topic/diagram/{id}/cursor-update { userId, x, y, color }
3. Other clients:          Update cursor position for that user
```

---

**← Previous:** `08-BACKEND-CONTROLLERS.md` | **Next:** `10-WEBSOCKET-HANDLERS.md` →
