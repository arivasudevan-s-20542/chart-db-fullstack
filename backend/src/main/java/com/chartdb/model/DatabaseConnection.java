package com.chartdb.model;

import com.chartdb.model.enums.ConnectionStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "database_connections", indexes = {
    @Index(name = "idx_database_connections_diagram", columnList = "diagram_id"),
    @Index(name = "idx_database_connections_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DatabaseConnection extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "database_type", nullable = false, length = 50)
    private String databaseType;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(name = "database_name", nullable = false, length = 100)
    private String databaseName;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;
    
    @Column(name = "ssl_enabled")
    @Builder.Default
    private Boolean sslEnabled = true;
    
    @Type(JsonBinaryType.class)
    @Column(name = "additional_params", columnDefinition = "jsonb")
    private Map<String, Object> additionalParams;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.UNKNOWN;
    
    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
