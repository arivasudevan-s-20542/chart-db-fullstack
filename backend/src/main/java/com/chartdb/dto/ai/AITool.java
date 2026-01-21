package com.chartdb.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AITool {
    private String name;
    private String description;
    private Map<String, Object> parameters;
    
    // Helper to create a tool definition
    public static AITool create(String name, String description, Map<String, Object> parameters) {
        return AITool.builder()
            .name(name)
            .description(description)
            .parameters(parameters)
            .build();
    }
}
