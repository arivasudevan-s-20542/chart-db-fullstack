package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionMessage {
    
    private String diagramId;
    private String userId;
    private String sessionId;
    private String tableId;
    private String columnId;
}
