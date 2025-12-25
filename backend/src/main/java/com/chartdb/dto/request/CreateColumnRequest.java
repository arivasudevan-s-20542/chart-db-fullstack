package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {
    
    // Optional: client can provide an ID, otherwise server generates one
    @Size(max = 36, message = "ID must not exceed 36 characters")
    private String id;
    
    @NotBlank(message = "Column name is required")
    @Size(max = 255, message = "Column name must not exceed 255 characters")
    private String name;
    
    // Backend field name - data_type column
    @Size(max = 100, message = "Data type must not exceed 100 characters")
    private String dataType;
    
    // Frontend sends "type" instead of "dataType"
    @Size(max = 100, message = "Data type must not exceed 100 characters")
    private String type;
    
    // Helper method to get effective data type
    public String getEffectiveDataType() {
        if (dataType != null && !dataType.isEmpty()) {
            return dataType;
        }
        return type;
    }
    
    private Boolean isPrimaryKey;
    
    private Boolean isForeignKey;
    
    private Boolean isNullable;
    
    private Boolean isUnique;
    
    private Boolean isAutoIncrement;
    
    private String defaultValue;
    
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
    
    private String checkConstraint;
    
    private Integer orderIndex;
    
    private String fkTableId;
    
    private String fkColumnId;
}
