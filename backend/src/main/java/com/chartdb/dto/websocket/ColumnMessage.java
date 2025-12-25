package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMessage {
    private String diagramId;
    private String tableId;
    private String columnId;
    private String action; // "created", "updated", "deleted"
    private String name;
    private String dataType;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isNullable;
    private Boolean isUnique;
    private String defaultValue;
    private Integer orderIndex;
    private String userId;
    private String userName;
    private Long timestamp;
}
