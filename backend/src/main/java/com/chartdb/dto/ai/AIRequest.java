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
public class AIRequest {
    private List<AIMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private List<AITool> tools;  // Function definitions for agent mode
    private Map<String, Object> additionalParams;
}
