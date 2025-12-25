package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResponse {
    
    private String id;
    private String diagramId;
    private String name;
    private String schemaName;
    private String description;
    
    // Position (CRITICAL for canvas)
    private BigDecimal positionX;
    private BigDecimal positionY;
    
    // Dimensions
    private BigDecimal width;
    private BigDecimal height;
    
    // Display
    private String color;
    private Boolean isCollapsed;
    private Boolean isHidden;
    private Integer sortOrder;
    private Integer zIndex;
    
    // View flags
    private Boolean isView;
    private Boolean isMaterializedView;
    
    // Columns
    private List<ColumnResponse> columns;
    
    // Indexes as JSON string
    private String indexes;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
