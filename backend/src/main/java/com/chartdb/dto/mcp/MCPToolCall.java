package com.chartdb.dto.mcp;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Tool Call Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPToolCall {
    @NotBlank(message = "Tool name is required")
    private String name;
    
    private Map<String, Object> arguments;
}
