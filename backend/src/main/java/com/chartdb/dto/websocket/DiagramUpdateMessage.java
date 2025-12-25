package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramUpdateMessage {
    
    private String type;
    private String diagramId;
    private String entityType;
    private String entityId;
    private String action; // CREATE, UPDATE, DELETE, MOVE
    
    // User who made the change
    private String userId;
    private String userName;
    private String userDisplayName;
    
    // Updated entity data
    private Map<String, Object> data;
    
    // For position updates
    private BigDecimal x;
    private BigDecimal y;
    
    private Long timestamp;
}
