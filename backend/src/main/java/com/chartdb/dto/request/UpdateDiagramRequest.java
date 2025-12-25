package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiagramRequest {
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    private String databaseType;
    
    private Boolean isPublic;
    
    private Boolean isTemplate;
    
    private BigDecimal canvasZoom;
    
    private BigDecimal canvasOffsetX;
    
    private BigDecimal canvasOffsetY;
    
    private Map<String, Object> metadata;
}
