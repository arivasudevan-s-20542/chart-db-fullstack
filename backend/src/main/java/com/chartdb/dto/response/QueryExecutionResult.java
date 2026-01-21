package com.chartdb.dto.response;

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
public class QueryExecutionResult {
    private boolean success;
    private String message;
    private List<Map<String, String>> columns;
    private List<List<Object>> rows;
    private Integer rowsAffected;
    private Long executionTimeMs;
    private String errorCode;
}
