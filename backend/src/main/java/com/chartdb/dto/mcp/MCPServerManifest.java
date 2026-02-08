package com.chartdb.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP Server Discovery Manifest
 * Describes the capabilities and available tools of the MCP server
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPServerManifest {
    private String name;
    private String version;
    private String description;
    private MCPCapabilities capabilities;
    private List<MCPTool> tools;
    private List<MCPResource> resources;
    private List<MCPPrompt> prompts;
}
