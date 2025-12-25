package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class DependencyResponse {
    private String id;
    private String diagramId;
    private String sourceTableId;
    private String targetTableId;
    private String dependencyType;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
