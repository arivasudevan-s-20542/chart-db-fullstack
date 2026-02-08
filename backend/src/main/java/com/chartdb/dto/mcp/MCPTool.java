package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Tool Definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPTool {
    private String name;
    private String description;
    private Map<String, MCPParameter> parameters;
    
    public static MCPTool of(String name, String description, Map<String, MCPParameter> parameters) {
        return MCPTool.builder()
            .name(name)
            .description(description)
            .parameters(parameters)
            .build();
    }
}
