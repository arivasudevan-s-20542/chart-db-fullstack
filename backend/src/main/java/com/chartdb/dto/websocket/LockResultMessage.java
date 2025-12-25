package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockResultMessage {
    private boolean acquired;
    private String tableId;
    private String lockedByUserId;
    private String lockedByUserName;
    private String message;
}
