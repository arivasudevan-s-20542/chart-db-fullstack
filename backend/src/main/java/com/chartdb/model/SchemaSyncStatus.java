package com.chartdb.model;

import com.chartdb.model.enums.SyncDirection;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "schema_sync_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SchemaSyncStatus extends BaseEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    @MapsId
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private DatabaseConnection connection;
    
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_direction", length = 20)
    private SyncDirection syncDirection;
    
    @Column(name = "tables_synced")
    private Integer tablesSynced;
    
    @Column(name = "drift_detected")
    @Builder.Default
    private Boolean driftDetected = false;
    
    @Type(JsonBinaryType.class)
    @Column(name = "drift_details", columnDefinition = "jsonb")
    private Map<String, Object> driftDetails;
    
    @Column(name = "next_auto_sync")
    private Instant nextAutoSync;
}
