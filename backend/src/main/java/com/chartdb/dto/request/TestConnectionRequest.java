package com.chartdb.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConnectionRequest {
    private String databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    
    @Builder.Default
    private Boolean sslEnabled = true;
}
