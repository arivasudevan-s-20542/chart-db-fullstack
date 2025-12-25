package com.chartdb.dto.request;

import com.chartdb.model.enums.Cardinality;
import com.chartdb.model.enums.RelationshipType;
import jakarta.validation.constraints.NotBlank;
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
public class CreateRelationshipRequest {
    
    // Optional: client can provide an ID, otherwise server generates one
    @Size(max = 36, message = "ID must not exceed 36 characters")
    private String id;
    
    @Size(max = 255, message = "Relationship name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Source table ID is required")
    private String sourceTableId;
    
    @NotBlank(message = "Target table ID is required")
    private String targetTableId;
    
    // Frontend sends sourceFieldId/targetFieldId, backend accepts both formats
    private String sourceColumnId;
    private String targetColumnId;
    private String sourceFieldId;
    private String targetFieldId;
    
    // Frontend sends cardinality (one/many), backend can also accept relationshipType
    private RelationshipType relationshipType;
    private Cardinality sourceCardinality;
    private Cardinality targetCardinality;
    
    private String sourceHandle;
    private String targetHandle;
    
    private List<Map<String, Object>> pathPoints;
    
    // Helper method to get source column ID from either field
    public String getEffectiveSourceColumnId() {
        return sourceColumnId != null ? sourceColumnId : sourceFieldId;
    }
    
    // Helper method to get target column ID from either field
    public String getEffectiveTargetColumnId() {
        return targetColumnId != null ? targetColumnId : targetFieldId;
    }
    
    // Helper method to determine relationship type from cardinality if not provided
    public RelationshipType getEffectiveRelationshipType() {
        if (relationshipType != null) {
            return relationshipType;
        }
        if (sourceCardinality != null && targetCardinality != null) {
            if (sourceCardinality == Cardinality.ONE && targetCardinality == Cardinality.ONE) {
                return RelationshipType.ONE_TO_ONE;
            } else if (sourceCardinality == Cardinality.ONE && targetCardinality == Cardinality.MANY) {
                return RelationshipType.ONE_TO_MANY;
            } else if (sourceCardinality == Cardinality.MANY && targetCardinality == Cardinality.ONE) {
                return RelationshipType.MANY_TO_ONE;
            } else {
                return RelationshipType.MANY_TO_MANY;
            }
        }
        return RelationshipType.ONE_TO_MANY; // Default
    }
}
