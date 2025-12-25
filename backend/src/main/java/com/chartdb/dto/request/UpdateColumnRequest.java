package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateColumnRequest {
    
    @Size(max = 255, message = "Column name must not exceed 255 characters")
    private String name;
    
    @Size(max = 100, message = "Data type must not exceed 100 characters")
    private String dataType;
    
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
