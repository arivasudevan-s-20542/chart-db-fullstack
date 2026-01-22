package com.chartdb.websocket;

import com.chartdb.dto.websocket.*;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DiagramWebSocketHandler {
    
    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // ═══════════════════════════════════════════════════════════════
    // JOIN / LEAVE DIAGRAM
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/join")
    public void joinDiagram(
            @DestinationVariable String diagramId,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        String sessionId = headerAccessor.getSessionId();
        collaborationService.joinDiagram(diagramId, user.getId(), sessionId);
        
        // Broadcast to other users that someone joined
        PresenceMessage presenceMessage = PresenceMessage.builder()
            .diagramId(diagramId)
            .userId(user.getId())
            .userDisplayName(user.getDisplayName())
            .userAvatarUrl(user.getAvatarUrl())
            .action("joined")
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", presenceMessage);
        log.info("User {} joined diagram {}", user.getId(), diagramId);
    }
    
    @MessageMapping("/diagram/{diagramId}/leave")
    public void leaveDiagram(
            @DestinationVariable String diagramId,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        collaborationService.leaveDiagram(diagramId, user.getId());
        
        // Broadcast to remaining users
        PresenceMessage presenceMessage = PresenceMessage.builder()
            .diagramId(diagramId)
            .userId(user.getId())
            .userDisplayName(user.getDisplayName())
            .action("left")
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", presenceMessage);
        log.info("User {} left diagram {}", user.getId(), diagramId);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CURSOR TRACKING
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/cursor")
    public void handleCursorMove(
            @DestinationVariable String diagramId,
            @Payload CursorMoveMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        // Update in database
        collaborationService.updateCursorPosition(diagramId, user.getId(), message.getX(), message.getY());
        
        // Broadcast to other users
        CursorBroadcast broadcast = CursorBroadcast.builder()
            .userId(user.getId())
            .userDisplayName(user.getDisplayName())
            .userAvatarUrl(user.getAvatarUrl())
            .cursorColor(user.getCursorColor())
            .x(message.getX())
            .y(message.getY())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/cursors", broadcast);
    }
    
    @MessageMapping("/diagram/{diagramId}/selection")
    public void handleSelection(
            @DestinationVariable String diagramId,
            @Payload SelectionMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        // Update in database
        collaborationService.updateSelection(diagramId, user.getId(), message.getTableId(), message.getColumnId());
        
        // Broadcast to other users
        SelectionBroadcast broadcast = SelectionBroadcast.builder()
            .userId(user.getId())
            .userDisplayName(user.getDisplayName())
            .cursorColor(user.getCursorColor())
            .tableId(message.getTableId())
            .columnId(message.getColumnId())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/selections", broadcast);
    }
    
    @MessageMapping("/diagram/{diagramId}/idle")
    public void handleIdleStatus(
            @DestinationVariable String diagramId,
            @Payload boolean idle,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        collaborationService.markIdle(diagramId, user.getId(), idle);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TABLE OPERATIONS (REAL-TIME SYNC)
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/table-move")
    public void handleTableMove(
            @DestinationVariable String diagramId,
            @Payload TableMoveMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setTimestamp(System.currentTimeMillis());
        
        // Broadcast position update to all collaborators
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-moved", message);
    }
    
    @MessageMapping("/diagram/{diagramId}/table-create")
    public void handleTableCreate(
            @DestinationVariable String diagramId,
            @Payload TableCreateMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setSessionId(headerAccessor.getSessionId());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-created", message);
        log.info("User {} created table {} in diagram {}", user.getId(), message.getTableId(), diagramId);
    }
    
    @MessageMapping("/diagram/{diagramId}/table-update")
    public void handleTableUpdate(
            @DestinationVariable String diagramId,
            @Payload TableUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setSessionId(headerAccessor.getSessionId());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-updated", message);
    }
    
    @MessageMapping("/diagram/{diagramId}/table-delete")
    public void handleTableDelete(
            @DestinationVariable String diagramId,
            @Payload TableDeleteMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setSessionId(headerAccessor.getSessionId());
        message.setTimestamp(System.currentTimeMillis());
        
        // Release any locks on this table
        collaborationService.unlockTable(diagramId, message.getTableId());
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-deleted", message);
        log.info("User {} deleted table {} from diagram {}", user.getId(), message.getTableId(), diagramId);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COLUMN OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/column-change")
    public void handleColumnChange(
            @DestinationVariable String diagramId,
            @Payload ColumnMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setSessionId(headerAccessor.getSessionId());
        message.setTimestamp(System.currentTimeMillis());
        
        String destination = String.format("/topic/diagram/%s/column-%s", diagramId, message.getAction());
        messagingTemplate.convertAndSend(destination, message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // RELATIONSHIP OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/relationship-change")
    public void handleRelationshipChange(
            @DestinationVariable String diagramId,
            @Payload RelationshipMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setTimestamp(System.currentTimeMillis());
        
        String destination = String.format("/topic/diagram/%s/relationship-%s", diagramId, message.getAction());
        messagingTemplate.convertAndSend(destination, message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LOCKING MECHANISM
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/lock-table")
    @SendToUser("/queue/lock-result")
    public LockResultMessage requestTableLock(
            @DestinationVariable String diagramId,
            @Payload LockMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) {
            return LockResultMessage.builder()
                .acquired(false)
                .tableId(message.getTableId())
                .message("Authentication required")
                .build();
        }
        
        boolean acquired = collaborationService.lockTable(diagramId, message.getTableId(), user.getId());
        
        if (acquired) {
            // Broadcast lock to all collaborators
            LockMessage broadcastMessage = LockMessage.builder()
                .diagramId(diagramId)
                .tableId(message.getTableId())
                .userId(user.getId())
                .userName(user.getDisplayName())
                .action("locked")
                .timestamp(System.currentTimeMillis())
                .build();
            
            messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-locked", broadcastMessage);
            
            return LockResultMessage.builder()
                .acquired(true)
                .tableId(message.getTableId())
                .lockedByUserId(user.getId())
                .lockedByUserName(user.getDisplayName())
                .message("Lock acquired")
                .build();
        }
        
        return LockResultMessage.builder()
            .acquired(false)
            .tableId(message.getTableId())
            .message("Table is locked by another user")
            .build();
    }
    
    @MessageMapping("/diagram/{diagramId}/unlock-table")
    public void releaseTableLock(
            @DestinationVariable String diagramId,
            @Payload LockMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        collaborationService.unlockTable(diagramId, message.getTableId(), user.getId());
        
        // Broadcast unlock to all collaborators
        LockMessage broadcastMessage = LockMessage.builder()
            .diagramId(diagramId)
            .tableId(message.getTableId())
            .userId(user.getId())
            .userName(user.getDisplayName())
            .action("unlocked")
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/table-unlocked", broadcastMessage);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DIAGRAM UPDATES
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/update")
    public void handleDiagramUpdate(
            @DestinationVariable String diagramId,
            @Payload DiagramUpdateMessage message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        message.setUserId(user.getId());
        message.setUserName(user.getDisplayName());
        message.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/updated", message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GENERIC EVENT HANDLER (for real-time collaboration)
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/diagram/{diagramId}/event")
    public void handleGenericEvent(
            @DestinationVariable String diagramId,
            @Payload java.util.Map<String, Object> message,
            Principal principal) {
        
        UserPrincipal user = extractUserPrincipal(principal);
        if (user == null) return;
        
        // Add user information to the message
        message.put("userId", user.getId());
        message.put("userEmail", user.getEmail());
        message.put("userDisplayName", user.getDisplayName());
        message.put("diagramId", diagramId);
        message.put("timestamp", java.time.Instant.now().toString());
        
        // Broadcast to all subscribers of this diagram's events topic
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/events", message);
        log.debug("Broadcasting event {} from user {} to diagram {}", message.get("type"), user.getId(), diagramId);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PING/PONG (Latency Measurement)
    // ═══════════════════════════════════════════════════════════════
    
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public java.util.Map<String, String> handlePing(@Payload java.util.Map<String, String> payload) {
        // Simply echo back the pingId so the client can calculate round-trip latency
        return java.util.Map.of("pingId", payload.getOrDefault("pingId", ""));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════
    
    private UserPrincipal extractUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object credentials = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (credentials instanceof UserPrincipal) {
                return (UserPrincipal) credentials;
            }
        }
        return null;
    }
}
