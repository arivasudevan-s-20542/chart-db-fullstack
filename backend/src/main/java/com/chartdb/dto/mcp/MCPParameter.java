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
    /**
     * For array types: defines the schema of each item.
     * Key-value pairs where keys are JSON Schema properties (e.g. "type", "properties").
     */
    private java.util.Map<String, Object> items;
    
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
    
    public static MCPParameter requiredArray(String description, java.util.Map<String, Object> items) {
        return MCPParameter.builder()
            .type("array")
            .description(description)
            .required(true)
            .items(items)
            .build();
    }
    
    public static MCPParameter optionalArray(String description, java.util.Map<String, Object> items) {
        return MCPParameter.builder()
            .type("array")
            .description(description)
            .required(false)
            .items(items)
            .build();
    }
}
