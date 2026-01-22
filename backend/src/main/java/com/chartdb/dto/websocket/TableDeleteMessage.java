package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDeleteMessage {
    private String diagramId;
    private String tableId;
    private String userId;
    private String userName;
    private String sessionId;
    private Long timestamp;
}
