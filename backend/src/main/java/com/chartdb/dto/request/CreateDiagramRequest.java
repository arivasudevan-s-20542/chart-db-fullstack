package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiagramRequest {
    
    // Optional: client can provide an ID, otherwise server generates one
    @Size(max = 36, message = "ID must not exceed 36 characters")
    private String id;
    
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    private String databaseType;
    
    private Boolean isPublic;
    
    private Boolean isTemplate;
    
    private Map<String, Object> metadata;
}
