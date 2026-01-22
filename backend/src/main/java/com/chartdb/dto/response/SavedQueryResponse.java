package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedQueryResponse {
    private String id;
    private String name;
    private String description;
    private String query;
    private String tags;
    private Boolean isPublic;
    private Integer usageCount;
    private Instant createdAt;
}
