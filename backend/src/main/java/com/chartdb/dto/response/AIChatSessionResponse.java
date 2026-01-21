package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatSessionResponse {
    private String id;
    private String diagramId;
    private Map<String, Object> agentConfig;
    private Integer messageCount;
    private Boolean isActive;
    private Instant startedAt;
    private Instant lastMessageAt;
}
