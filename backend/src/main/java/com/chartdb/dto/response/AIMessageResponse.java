package com.chartdb.dto.response;

import com.chartdb.model.enums.MessageRole;
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
public class AIMessageResponse {
    private String id;
    private MessageRole role;
    private String content;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
