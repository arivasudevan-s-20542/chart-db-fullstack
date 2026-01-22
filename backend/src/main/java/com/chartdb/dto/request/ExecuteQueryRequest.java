package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteQueryRequest {
    
    @NotBlank(message = "Query is required")
    private String query;
    
    @Builder.Default
    private Integer maxRows = 1000;
    
    @Builder.Default
    private Integer timeoutSeconds = 30;
}
