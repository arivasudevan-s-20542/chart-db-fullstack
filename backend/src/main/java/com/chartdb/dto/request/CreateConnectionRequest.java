package com.chartdb.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConnectionRequest {
    
    @NotBlank(message = "Diagram ID is required")
    private String diagramId;
    
    @NotBlank(message = "Connection name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;
    
    @NotBlank(message = "Database type is required")
    private String databaseType; // postgresql, mysql, sqlserver, oracle
    
    @NotBlank(message = "Host is required")
    private String host;
    
    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private Integer port;
    
    @NotBlank(message = "Database name is required")
    private String databaseName;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @Builder.Default
    private Boolean sslEnabled = true;
    
    private Map<String, Object> additionalParams;
}
