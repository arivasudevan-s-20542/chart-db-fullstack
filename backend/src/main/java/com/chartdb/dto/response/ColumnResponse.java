package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnResponse {
    
    private String id;
    private String tableId;
    private String name;
    private String dataType;
    
    // Constraints
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isNullable;
    private Boolean isUnique;
    private Boolean isAutoIncrement;
    
    // Additional properties
    private String defaultValue;
    private String comment;
    private String checkConstraint;
    private Integer orderIndex;
    
    // Foreign key references
    private String fkTableId;
    private String fkTableName;
    private String fkColumnId;
    private String fkColumnName;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
