package com.chartdb.dto.response;

import com.chartdb.model.enums.QueryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistoryResponse {
    private String id;
    private String query;
    private Integer executionTimeMs;
    private Integer rowsAffected;
    private QueryStatus status;
    private String errorMessage;
    private Instant executedAt;
}
