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
public class DiagramSummaryResponse {
    
    private String id;
    private String name;
    private String description;
    private String databaseType;
    private String status;
    
    // Owner info
    private String ownerId;
    private String ownerDisplayName;
    private String ownerAvatarUrl;
    
    // Settings
    private Boolean isPublic;
    private Boolean isTemplate;
    
    // Quick stats
    private Integer tableCount;
    private Integer relationshipCount;
    
    // Stats
    private Integer viewCount;
    private Integer exportCount;
    
    // Timestamps
    private Instant lastAccessedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
