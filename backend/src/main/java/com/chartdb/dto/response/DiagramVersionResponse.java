package com.chartdb.dto.response;

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
public class DiagramVersionResponse {
    
    private String id;
    private String diagramId;
    private Integer versionNumber;
    
    // Metadata
    private String name;
    private String description;
    
    // Creator info
    private String createdById;
    private String createdByDisplayName;
    private String createdByAvatarUrl;
    
    // Status
    private Boolean isCurrent;
    private Boolean isAutoSave;
    
    // Snapshot (optional - not included in list responses)
    private Map<String, Object> snapshotData;
    
    private Instant createdAt;
}
