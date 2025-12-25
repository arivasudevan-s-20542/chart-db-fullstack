package com.chartdb.dto.request;

import com.chartdb.model.enums.RelationshipType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRelationshipRequest {
    
    @Size(max = 255, message = "Relationship name must not exceed 255 characters")
    private String name;
    
    private RelationshipType relationshipType;
    
    private String sourceHandle;
    
    private String targetHandle;
    
    private List<Map<String, Object>> pathPoints;
}
