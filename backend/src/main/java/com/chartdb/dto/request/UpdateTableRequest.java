package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTableRequest {
    
    @Size(max = 255, message = "Table name must not exceed 255 characters")
    private String name;
    
    private String schema;
    
    // Also accept 'schemaName' from frontend
    private String schemaName;
    
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
    
    // Also accept 'description' from frontend
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    public String getEffectiveComment() {
        if (comment != null && !comment.isEmpty()) {
            return comment;
        }
        return description;
    }
    
    public String getEffectiveSchema() {
        if (schema != null && !schema.isEmpty()) {
            return schema;
        }
        return schemaName;
    }
    
    private BigDecimal positionX;
    
    private BigDecimal positionY;
    
    private BigDecimal width;
    
    private BigDecimal height;
    
    private String color;
    
    private Boolean isCollapsed;
    
    private Boolean isHidden;
    
    private Integer sortOrder;
    
    private Integer zIndex;
    
    private Boolean isView;
    
    private Boolean isMaterializedView;
    
    // Columns/fields - when provided, sync all columns
    private List<CreateColumnRequest> columns;
    
    // Indexes - stored as JSON string
    private String indexes;
}
