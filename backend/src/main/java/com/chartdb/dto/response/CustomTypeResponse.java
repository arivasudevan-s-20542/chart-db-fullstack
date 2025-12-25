package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class CustomTypeResponse {
    private String id;
    private String diagramId;
    private String name;
    private String kind;
    private String schemaName;
    private String values;
    private String attributes;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
