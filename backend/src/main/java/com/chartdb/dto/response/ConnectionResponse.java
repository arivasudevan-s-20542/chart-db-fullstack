package com.chartdb.dto.response;

import com.chartdb.model.enums.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {
    private String id;
    private String diagramId;
    private String name;
    private String databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    // Password is never returned
    private Boolean sslEnabled;
    private ConnectionStatus status;
    private Instant lastConnectedAt;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
