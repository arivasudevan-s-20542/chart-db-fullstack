package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramFullResponse {
    
    private String id;
    private String name;
    private String description;
    private String databaseType;
    private Boolean isPublic;
    
    // Canvas state
    private BigDecimal zoom;
    private BigDecimal offsetX;
    private BigDecimal offsetY;
    
    // Statistics
    private Integer tableCount;
    private Integer relationshipCount;
    
    // Owner info
    private String ownerId;
    private String ownerDisplayName;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    
    // Full content
    private List<TableResponse> tables;
    private List<RelationshipResponse> relationships;
    
    // Current user's permission level
    private String permissionLevel;
    
    // Metadata
    private Map<String, Object> metadata;
}
