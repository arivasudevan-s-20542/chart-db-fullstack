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
public class TableCreateMessage {
    private String diagramId;
    private String tableId;
    private String name;
    private String displayName;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private String color;
    private List<ColumnData> columns;
    private String userId;
    private String userName;
    private Long timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnData {
        private String id;
        private String name;
        private String dataType;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private Boolean isNullable;
        private Boolean isUnique;
        private String defaultValue;
    }
}
