package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorResponse {
    
    private String id;
    private String sessionId;
    private String diagramId;
    
    // User info
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;
    private String cursorColor;
    
    // Position
    private BigDecimal cursorX;
    private BigDecimal cursorY;
    
    // Selection state
    private String selectedTableId;
    private String selectedTableName;
    private String selectedColumnId;
    
    // Status
    private Boolean isActive;
    private Boolean isIdle;
    private String status;
    
    // Timestamps
    private Instant joinedAt;
    private Instant lastActivity;
}
