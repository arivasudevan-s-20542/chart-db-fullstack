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
public class SaveQueryRequest {
    
    @NotBlank(message = "Query name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Query is required")
    private String query;
    
    private String tags;
    
    @Builder.Default
    private Boolean isPublic = false;
}
