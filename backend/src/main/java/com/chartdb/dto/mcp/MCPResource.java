package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Resource Definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPResource {
    private String uri;
    private String description;
    private String mimeType;
    
    public static MCPResource of(String uri, String description, String mimeType) {
        return MCPResource.builder()
            .uri(uri)
            .description(description)
            .mimeType(mimeType)
            .build();
    }
}
