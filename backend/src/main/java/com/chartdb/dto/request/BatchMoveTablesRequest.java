package com.chartdb.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMoveTablesRequest {
    
    @NotEmpty(message = "Table IDs are required")
    private List<String> tableIds;
    
    @NotNull(message = "Delta X is required")
    private BigDecimal deltaX;
    
    @NotNull(message = "Delta Y is required")
    private BigDecimal deltaY;
}
