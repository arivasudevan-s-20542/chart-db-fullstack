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
public class CursorBroadcast {
    
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;
    private String cursorColor;
    private BigDecimal x;
    private BigDecimal y;
}
