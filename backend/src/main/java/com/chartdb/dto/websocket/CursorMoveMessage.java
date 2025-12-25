package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorMoveMessage {
    
    private String diagramId;
    private String userId;
    private String sessionId;
    private BigDecimal x;
    private BigDecimal y;
}
