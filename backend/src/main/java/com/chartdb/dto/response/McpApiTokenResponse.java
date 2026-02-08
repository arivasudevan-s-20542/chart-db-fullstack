package com.chartdb.dto.response;

import com.chartdb.model.McpApiToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpApiTokenResponse {
    
    private String id;
    private String name;
    private String tokenPrefix;
    private String scopes;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private boolean active;
    private Instant createdAt;
    
    /**
     * The plain-text token - ONLY populated at creation time.
     * After creation, this will be null.
     */
    private String token;
    
    public static McpApiTokenResponse fromEntity(McpApiToken entity) {
        return McpApiTokenResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .tokenPrefix(entity.getTokenPrefix())
            .scopes(entity.getScopes())
            .lastUsedAt(entity.getLastUsedAt())
            .expiresAt(entity.getExpiresAt())
            .active(entity.isValid())
            .createdAt(entity.getCreatedAt())
            .build();
    }
    
    public static McpApiTokenResponse fromEntityWithToken(McpApiToken entity, String plainTextToken) {
        McpApiTokenResponse response = fromEntity(entity);
        response.setToken(plainTextToken);
        return response;
    }
}
