package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableUpdateMessage {
    private String diagramId;
    private String tableId;
    private String name;
    private String displayName;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private String color;
    private Map<String, Object> metadata;
    private String userId;
    private String userName;
    private Long timestamp;
}
