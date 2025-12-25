# 10 - WebSocket Handlers (Controllers)

## 🎮 Collaboration Controller

```java
package com.chartdb.controller;

import com.chartdb.dto.CollaboratorDTO;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.CollaborationService;
import com.chartdb.websocket.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CollaborationWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final CollaborationService collaborationService;
    
    // ═══════════════════════════════════════════════════════════════
    // JOIN / LEAVE DIAGRAM
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle user joining a diagram for real-time collaboration
     */
    @MessageMapping("/join-diagram")
    @SendToUser("/queue/join-result")
    public List<CollaboratorDTO> joinDiagram(
            @Payload JoinDiagramMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        String sessionId = headerAccessor.getSessionId();
        UserPrincipal user = extractUser(principal);
        
        log.info("User {} joining diagram {}", user.getId(), message.getDiagramId());
        
        // Register collaborator
        CollaboratorDTO collaborator = collaborationService.joinDiagram(
            message.getDiagramId(),
            user.getId(),
            sessionId
        );
        
        // Broadcast to other users in the diagram
        JoinDiagramMessage broadcastMessage = JoinDiagramMessage.builder()
            .diagramId(message.getDiagramId())
            .userId(user.getId().toString())
            .userName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .cursorColor(collaborator.getCursorColor())
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/user-joined",
            broadcastMessage
        );
        
        // Return current collaborators to the joining user
        return collaborationService.getActiveCollaborators(message.getDiagramId());
    }
    
    /**
     * Handle user leaving a diagram
     */
    @MessageMapping("/leave-diagram")
    public void leaveDiagram(
            @Payload LeaveDiagramMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        
        log.info("User {} leaving diagram {}", user.getId(), message.getDiagramId());
        
        collaborationService.leaveDiagram(message.getDiagramId(), user.getId());
        
        // Broadcast to remaining users
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/user-left",
            LeaveDiagramMessage.builder()
                .diagramId(message.getDiagramId())
                .userId(user.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CURSOR TRACKING
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle cursor movement - broadcast to all collaborators
     */
    @MessageMapping("/cursor-move")
    public void handleCursorMove(
            @Payload CursorMoveMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        
        // Add user info to message
        message.setUserId(user.getId().toString());
        message.setUserName(user.getDisplayName());
        message.setTimestamp(System.currentTimeMillis());
        
        // Get user's cursor color
        String cursorColor = collaborationService.getUserCursorColor(
            message.getDiagramId(), 
            user.getId()
        );
        message.setColor(cursorColor);
        
        // Broadcast to all in diagram except sender
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/cursor-update",
            message
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TABLE OPERATIONS (REAL-TIME SYNC)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle table position change during drag
     */
    @MessageMapping("/table-move")
    public void handleTableMove(
            @Payload TableMoveMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setTimestamp(System.currentTimeMillis());
        
        // Broadcast position update to all collaborators
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/table-moved",
            message
        );
    }
    
    /**
     * Broadcast new table creation
     */
    @MessageMapping("/table-create")
    public void handleTableCreate(
            @Payload TableCreateMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/table-created",
            message
        );
    }
    
    /**
     * Broadcast table update
     */
    @MessageMapping("/table-update")
    public void handleTableUpdate(
            @Payload TableUpdateMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/table-updated",
            message
        );
    }
    
    /**
     * Broadcast table deletion
     */
    @MessageMapping("/table-delete")
    public void handleTableDelete(
            @Payload TableDeleteMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setTimestamp(System.currentTimeMillis());
        
        // Release any locks on this table
        collaborationService.unlockTable(message.getDiagramId(), message.getTableId());
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/table-deleted",
            message
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COLUMN OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle column changes (created, updated, deleted)
     */
    @MessageMapping("/column-change")
    public void handleColumnChange(
            @Payload ColumnMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setTimestamp(System.currentTimeMillis());
        
        String destination = String.format(
            "/topic/diagram/%s/column-%s",
            message.getDiagramId(),
            message.getAction()  // created, updated, deleted
        );
        
        messagingTemplate.convertAndSend(destination, message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LOCKING MECHANISM
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Request lock on a table for editing
     */
    @MessageMapping("/lock-table")
    @SendToUser("/queue/lock-result")
    public LockResult requestTableLock(
            @Payload LockMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        
        boolean acquired = collaborationService.lockTable(
            message.getDiagramId(),
            message.getTableId(),
            user.getId()
        );
        
        if (acquired) {
            // Broadcast lock to all collaborators
            LockMessage broadcastMessage = LockMessage.builder()
                .diagramId(message.getDiagramId())
                .tableId(message.getTableId())
                .userId(user.getId().toString())
                .userName(user.getDisplayName())
                .action("locked")
                .timestamp(System.currentTimeMillis())
                .build();
            
            messagingTemplate.convertAndSend(
                "/topic/diagram/" + message.getDiagramId() + "/table-locked",
                broadcastMessage
            );
        }
        
        return new LockResult(acquired, message.getTableId());
    }
    
    /**
     * Release lock on a table
     */
    @MessageMapping("/unlock-table")
    public void releaseTableLock(
            @Payload LockMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        
        collaborationService.unlockTable(
            message.getDiagramId(),
            message.getTableId(),
            user.getId()
        );
        
        // Broadcast unlock to all collaborators
        LockMessage broadcastMessage = LockMessage.builder()
            .diagramId(message.getDiagramId())
            .tableId(message.getTableId())
            .userId(user.getId().toString())
            .action("unlocked")
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/table-unlocked",
            broadcastMessage
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SELECTION AWARENESS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Broadcast user's current selection
     */
    @MessageMapping("/select")
    public void handleSelection(
            @Payload SelectionMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUser(principal);
        message.setUserId(user.getId().toString());
        message.setUserName(user.getDisplayName());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(
            "/topic/diagram/" + message.getDiagramId() + "/selection-changed",
            message
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════
    
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorMessage handleException(Exception e, Principal principal) {
        log.error("WebSocket error for user {}: {}", 
            principal != null ? principal.getName() : "unknown", 
            e.getMessage(), e);
        
        return ErrorMessage.builder()
            .error(e.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════
    
    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            return (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        throw new RuntimeException("User not authenticated");
    }
}
```

---

## 📦 Supporting DTOs

```java
// LockResult.java
package com.chartdb.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LockResult {
    private boolean acquired;
    private String tableId;
}

// ErrorMessage.java
package com.chartdb.websocket.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorMessage {
    private String error;
    private long timestamp;
}
```

---

## 🤝 Collaboration Service

```java
package com.chartdb.service;

import com.chartdb.dto.CollaboratorDTO;
import com.chartdb.entity.ActiveCollaborator;
import com.chartdb.entity.User;
import com.chartdb.repository.ActiveCollaboratorRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {
    
    private final ActiveCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    
    // Color palette for cursor/presence indicators
    private static final String[] CURSOR_COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", 
        "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
        "#BB8FCE", "#85C1E9", "#F1948A", "#82E0AA"
    };
    
    // In-memory lock tracking (for production, use Redis)
    private final Map<String, String> tableLocks = new ConcurrentHashMap<>();
    
    /**
     * Add user as active collaborator on a diagram
     */
    @Transactional
    public CollaboratorDTO joinDiagram(String diagramId, UUID userId, String websocketSessionId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if already collaborating
        Optional<ActiveCollaborator> existing = collaboratorRepository
            .findByDiagramIdAndUserId(diagramId, userId);
        
        if (existing.isPresent()) {
            // Update session
            ActiveCollaborator collaborator = existing.get();
            collaborator.setWebsocketSessionId(websocketSessionId);
            collaborator.setLastActiveAt(LocalDateTime.now());
            collaboratorRepository.save(collaborator);
            return toDTO(collaborator, user);
        }
        
        // Assign unique color
        int collaboratorCount = collaboratorRepository.countByDiagramId(diagramId);
        String cursorColor = CURSOR_COLORS[collaboratorCount % CURSOR_COLORS.length];
        
        ActiveCollaborator collaborator = ActiveCollaborator.builder()
            .diagramId(diagramId)
            .userId(userId)
            .websocketSessionId(websocketSessionId)
            .cursorColor(cursorColor)
            .joinedAt(LocalDateTime.now())
            .lastActiveAt(LocalDateTime.now())
            .build();
        
        collaboratorRepository.save(collaborator);
        log.info("User {} joined diagram {}", userId, diagramId);
        
        return toDTO(collaborator, user);
    }
    
    /**
     * Remove user from active collaborators
     */
    @Transactional
    public void leaveDiagram(String diagramId, UUID userId) {
        collaboratorRepository.deleteByDiagramIdAndUserId(diagramId, userId);
        
        // Release any locks held by this user
        releaseUserLocks(diagramId, userId.toString());
        
        log.info("User {} left diagram {}", userId, diagramId);
    }
    
    /**
     * Remove by WebSocket session (disconnect event)
     */
    @Transactional
    public void leaveByWebsocketSession(String sessionId) {
        Optional<ActiveCollaborator> collaborator = collaboratorRepository
            .findByWebsocketSessionId(sessionId);
        
        collaborator.ifPresent(c -> {
            String userId = c.getUserId().toString();
            String diagramId = c.getDiagramId();
            
            collaboratorRepository.delete(c);
            releaseUserLocks(diagramId, userId);
            
            log.info("User {} disconnected from diagram {}", userId, diagramId);
        });
    }
    
    /**
     * Get all active collaborators for a diagram
     */
    public List<CollaboratorDTO> getActiveCollaborators(String diagramId) {
        List<ActiveCollaborator> collaborators = collaboratorRepository.findByDiagramId(diagramId);
        
        return collaborators.stream()
            .map(c -> {
                User user = userRepository.findById(c.getUserId()).orElse(null);
                return toDTO(c, user);
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Get cursor color for a user
     */
    public String getUserCursorColor(String diagramId, UUID userId) {
        return collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .map(ActiveCollaborator::getCursorColor)
            .orElse(CURSOR_COLORS[0]);
    }
    
    /**
     * Lock a table for editing
     */
    public boolean lockTable(String diagramId, String tableId, UUID userId) {
        String lockKey = diagramId + ":" + tableId;
        String userIdStr = userId.toString();
        
        return tableLocks.putIfAbsent(lockKey, userIdStr) == null 
            || tableLocks.get(lockKey).equals(userIdStr);
    }
    
    /**
     * Unlock a table
     */
    public void unlockTable(String diagramId, String tableId, UUID userId) {
        String lockKey = diagramId + ":" + tableId;
        String currentHolder = tableLocks.get(lockKey);
        
        if (currentHolder != null && currentHolder.equals(userId.toString())) {
            tableLocks.remove(lockKey);
        }
    }
    
    /**
     * Unlock a table (without user check)
     */
    public void unlockTable(String diagramId, String tableId) {
        String lockKey = diagramId + ":" + tableId;
        tableLocks.remove(lockKey);
    }
    
    /**
     * Release all locks held by a user on a diagram
     */
    private void releaseUserLocks(String diagramId, String userId) {
        tableLocks.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(diagramId + ":") && 
            entry.getValue().equals(userId)
        );
    }
    
    /**
     * Update last active timestamp
     */
    @Transactional
    public void updateLastActive(String diagramId, UUID userId) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(c -> {
                c.setLastActiveAt(LocalDateTime.now());
                collaboratorRepository.save(c);
            });
    }
    
    /**
     * Clean up stale collaborators (no activity for 5+ minutes)
     */
    @Transactional
    public void cleanupStaleCollaborators() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        collaboratorRepository.deleteByLastActiveAtBefore(threshold);
    }
    
    private CollaboratorDTO toDTO(ActiveCollaborator collaborator, User user) {
        if (user == null) return null;
        
        return CollaboratorDTO.builder()
            .userId(collaborator.getUserId())
            .userName(user.getDisplayName())
            .email(user.getEmail())
            .avatarUrl(user.getAvatarUrl())
            .cursorColor(collaborator.getCursorColor())
            .joinedAt(collaborator.getJoinedAt())
            .lastActiveAt(collaborator.getLastActiveAt())
            .build();
    }
}
```

---

## 📊 CollaboratorDTO

```java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CollaboratorDTO {
    private UUID userId;
    private String userName;
    private String email;
    private String avatarUrl;
    private String cursorColor;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    
    // Optional: current position
    private Double cursorX;
    private Double cursorY;
    
    // Optional: current selection
    private String selectedTableId;
    private String selectedColumnId;
}
```

---

## ⏰ Scheduled Cleanup Task

```java
package com.chartdb.scheduler;

import com.chartdb.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollaboratorCleanupScheduler {
    
    private final CollaborationService collaborationService;
    
    // Run every minute to clean up stale collaborators
    @Scheduled(fixedRate = 60000)
    public void cleanupStaleCollaborators() {
        log.debug("Running stale collaborator cleanup");
        collaborationService.cleanupStaleCollaborators();
    }
}
```

---

**← Previous:** `09-WEBSOCKET-CONFIG.md` | **Next:** `11-DTOS-MAPPERS.md` →
