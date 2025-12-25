package com.chartdb.service;

import com.chartdb.dto.websocket.DiagramUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcastTableCreated(String diagramId, String userId, String userDisplayName, Map<String, Object> tableData) {
        broadcast(diagramId, "TABLE", tableData.get("id").toString(), "CREATE", userId, userDisplayName, tableData);
    }
    
    public void broadcastTableUpdated(String diagramId, String userId, String userDisplayName, Map<String, Object> tableData) {
        broadcast(diagramId, "TABLE", tableData.get("id").toString(), "UPDATE", userId, userDisplayName, tableData);
    }
    
    public void broadcastTableMoved(String diagramId, String userId, String userDisplayName, String tableId, Object x, Object y) {
        DiagramUpdateMessage message = DiagramUpdateMessage.builder()
            .type("DIAGRAM_UPDATE")
            .diagramId(diagramId)
            .entityType("TABLE")
            .entityId(tableId)
            .action("MOVE")
            .userId(userId)
            .userDisplayName(userDisplayName)
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/updates", message);
    }
    
    public void broadcastTableDeleted(String diagramId, String userId, String userDisplayName, String tableId) {
        broadcast(diagramId, "TABLE", tableId, "DELETE", userId, userDisplayName, null);
    }
    
    public void broadcastColumnCreated(String diagramId, String userId, String userDisplayName, Map<String, Object> columnData) {
        broadcast(diagramId, "COLUMN", columnData.get("id").toString(), "CREATE", userId, userDisplayName, columnData);
    }
    
    public void broadcastColumnUpdated(String diagramId, String userId, String userDisplayName, Map<String, Object> columnData) {
        broadcast(diagramId, "COLUMN", columnData.get("id").toString(), "UPDATE", userId, userDisplayName, columnData);
    }
    
    public void broadcastColumnDeleted(String diagramId, String userId, String userDisplayName, String columnId) {
        broadcast(diagramId, "COLUMN", columnId, "DELETE", userId, userDisplayName, null);
    }
    
    public void broadcastRelationshipCreated(String diagramId, String userId, String userDisplayName, Map<String, Object> relationshipData) {
        broadcast(diagramId, "RELATIONSHIP", relationshipData.get("id").toString(), "CREATE", userId, userDisplayName, relationshipData);
    }
    
    public void broadcastRelationshipUpdated(String diagramId, String userId, String userDisplayName, Map<String, Object> relationshipData) {
        broadcast(diagramId, "RELATIONSHIP", relationshipData.get("id").toString(), "UPDATE", userId, userDisplayName, relationshipData);
    }
    
    public void broadcastRelationshipDeleted(String diagramId, String userId, String userDisplayName, String relationshipId) {
        broadcast(diagramId, "RELATIONSHIP", relationshipId, "DELETE", userId, userDisplayName, null);
    }
    
    private void broadcast(String diagramId, String entityType, String entityId, String action, 
                          String userId, String userDisplayName, Map<String, Object> data) {
        DiagramUpdateMessage message = DiagramUpdateMessage.builder()
            .type("DIAGRAM_UPDATE")
            .diagramId(diagramId)
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .userId(userId)
            .userDisplayName(userDisplayName)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/updates", message);
        log.debug("Broadcast {} {} {} to diagram {}", action, entityType, entityId, diagramId);
    }
}
