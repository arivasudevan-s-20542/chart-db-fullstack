package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Tool Parameter Definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPParameter {
    private String type;
    private String description;
    private boolean required;
    
    public static MCPParameter required(String type, String description) {
        return MCPParameter.builder()
            .type(type)
            .description(description)
            .required(true)
            .build();
    }
    
    public static MCPParameter optional(String type, String description) {
        return MCPParameter.builder()
            .type(type)
            .description(description)
            .required(false)
            .build();
    }
}
