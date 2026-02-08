package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server Capabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPCapabilities {
    private boolean tools;
    private boolean resources;
    private boolean prompts;
}
