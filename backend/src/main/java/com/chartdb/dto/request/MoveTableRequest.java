package com.chartdb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveTableRequest {
    
    @NotNull(message = "Position X is required")
    private BigDecimal positionX;
    
    @NotNull(message = "Position Y is required")
    private BigDecimal positionY;
}
