package com.chartdb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String cursorColor;
    private Map<String, Object> preferences;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
