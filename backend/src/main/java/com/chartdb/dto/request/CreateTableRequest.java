package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateTableRequest {
    
    // Optional: client can provide an ID, otherwise server generates one
    @Size(max = 36, message = "ID must not exceed 36 characters")
    private String id;
    
    @NotBlank(message = "Table name is required")
    @Size(max = 255, message = "Table name must not exceed 255 characters")
    private String name;
    
    private String schema;
    
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
    
    private BigDecimal positionX;
    
    private BigDecimal positionY;
    
    private BigDecimal width;
    
    private BigDecimal height;
    
    private String color;
    
    private Boolean isCollapsed;
    
    private List<CreateColumnRequest> columns;
}
