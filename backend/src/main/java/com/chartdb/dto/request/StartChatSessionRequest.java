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
public class StartChatSessionRequest {
    
    @NotBlank(message = "Diagram ID is required")
    private String diagramId;
    
    private String agentId; // Optional - use default if not provided
}
