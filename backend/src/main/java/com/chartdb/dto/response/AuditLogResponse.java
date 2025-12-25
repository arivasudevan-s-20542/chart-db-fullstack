package com.chartdb.dto.response;

import com.chartdb.model.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private String id;
    private String diagramId;
    
    // User who performed the action
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;
    
    // Action details
    private ActionType actionType;
    private String entityType;
    private String entityId;
    private String entityName;
    
    // Changes
    private Map<String, Object> changes;
    
    private Instant createdAt;
}
