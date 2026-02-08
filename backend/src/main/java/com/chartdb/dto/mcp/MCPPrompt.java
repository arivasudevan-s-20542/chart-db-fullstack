package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Prompt Definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPPrompt {
    private String name;
    private String description;
    private Map<String, MCPParameter> parameters;
    
    public static MCPPrompt of(String name, String description, Map<String, MCPParameter> parameters) {
        return MCPPrompt.builder()
            .name(name)
            .description(description)
            .parameters(parameters)
            .build();
    }
}
