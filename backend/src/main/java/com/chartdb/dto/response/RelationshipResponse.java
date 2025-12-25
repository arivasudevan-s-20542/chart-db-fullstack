package com.chartdb.dto.response;

import com.chartdb.model.enums.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipResponse {
    
    private String id;
    private String diagramId;
    private String name;
    
    // Source
    private String sourceTableId;
    private String sourceTableName;
    private String sourceColumnId;
    private String sourceColumnName;
    
    // Target
    private String targetTableId;
    private String targetTableName;
    private String targetColumnId;
    private String targetColumnName;
    
    // Type and styling
    private RelationshipType relationshipType;
    private String sourceHandle;
    private String targetHandle;
    private List<Map<String, Object>> pathPoints;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
