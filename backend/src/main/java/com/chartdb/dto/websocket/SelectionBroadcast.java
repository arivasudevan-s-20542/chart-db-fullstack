package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionBroadcast {
    
    private String userId;
    private String userDisplayName;
    private String cursorColor;
    private String tableId;
    private String tableName;
    private String columnId;
}
