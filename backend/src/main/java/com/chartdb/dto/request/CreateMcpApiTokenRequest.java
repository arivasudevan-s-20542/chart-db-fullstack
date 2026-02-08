package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMcpApiTokenRequest {
    
    @NotBlank(message = "Token name is required")
    @Size(min = 1, max = 100, message = "Token name must be between 1 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Scopes must not exceed 500 characters")
    private String scopes;
    
    private Instant expiresAt;
}
