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
public class DiagramResponse {
    
    private String id;
    private String name;
    private String description;
    private String databaseType;
    private String status;
    
    // Owner info
    private String ownerId;
    private String ownerDisplayName;
    private String ownerAvatarUrl;
    
    // Canvas state
    private BigDecimal canvasZoom;
    private BigDecimal canvasOffsetX;
    private BigDecimal canvasOffsetY;
    
    // Settings
    private Boolean isPublic;
    private Boolean isTemplate;
    private String publicSlug;
    
    // Metadata
    private Map<String, Object> metadata;
    
    // Nested entities
    private List<TableResponse> tables;
    private List<RelationshipResponse> relationships;
    
    // Stats
    private Integer viewCount;
    private Integer exportCount;
    
    // Timestamps
    private Instant lastAccessedAt;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;
    
    // User's permission level for this diagram
    private String permissionLevel;
}
