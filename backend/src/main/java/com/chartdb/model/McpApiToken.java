package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "mcp_api_tokens", indexes = {
    @Index(name = "idx_mcp_api_tokens_token_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_mcp_api_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_mcp_api_tokens_prefix", columnList = "token_prefix")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class McpApiToken extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    
    @Column(name = "token_prefix", nullable = false, length = 12)
    private String tokenPrefix;
    
    @Column(length = 500)
    @Builder.Default
    private String scopes = "mcp:read,mcp:write";
    
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * Check if this token has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if this token is valid (active and not expired)
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }
}
