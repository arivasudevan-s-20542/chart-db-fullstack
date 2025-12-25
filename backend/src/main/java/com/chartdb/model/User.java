package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "cursor_color", length = 7)
    @Builder.Default
    private String cursorColor = "#3B82F6";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> preferences = Map.of();
    
    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";
    
    @Column(length = 10)
    @Builder.Default
    private String locale = "en-US";
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    // Helper method to get display name or username
    public String getEffectiveDisplayName() {
        return displayName != null ? displayName : username;
    }
}
