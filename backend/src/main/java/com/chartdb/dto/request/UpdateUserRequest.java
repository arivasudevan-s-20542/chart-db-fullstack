package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;
    
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    private String username;
    
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
    
    private Map<String, Object> preferences;
}
