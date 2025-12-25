package com.chartdb.dto.websocket;

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
public class TableMoveMessage {
    
    private String diagramId;
    private String tableId;
    private BigDecimal x;
    private BigDecimal y;
    
    // For batch moves
    private List<String> tableIds;
    private BigDecimal deltaX;
    private BigDecimal deltaY;
    
    // User info (set by handler)
    private String userId;
    private String userName;
    private Long timestamp;
}
