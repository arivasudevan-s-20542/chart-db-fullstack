package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableLockResponse {
    
    private String id;
    private String tableId;
    private String tableName;
    
    // Lock owner
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;
    
    // Lock details
    private Instant lockedAt;
    private Instant expiresAt;
}
