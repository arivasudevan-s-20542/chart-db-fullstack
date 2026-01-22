package com.chartdb.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String content;
    private String model;
    private Integer tokensUsed;
    private AIFunctionCall functionCall;  // Function call from AI
    private Map<String, Object> metadata;
}
