package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockMessage {
    
    private String type; // LOCK, UNLOCK, EXTEND, EXPIRED
    private String diagramId;
    private String tableId;
    private String userId;
    private String userName; // Alias for userDisplayName
    private String userDisplayName;
    private String cursorColor;
    private String action; // locked, unlocked
    private Instant expiresAt;
    private Long timestamp;
}
