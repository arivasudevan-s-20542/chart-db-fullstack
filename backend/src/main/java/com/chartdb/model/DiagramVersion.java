package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "diagram_versions", indexes = {
    @Index(name = "idx_versions_diagram", columnList = "diagram_id"),
    @Index(name = "idx_versions_created", columnList = "diagram_id, created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_version_number", columnNames = {"diagram_id", "version_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramVersion {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    // Version Info
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    @Column(name = "version_label", length = 100)
    private String versionLabel;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Snapshot (complete diagram state)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshotData;
    
    // Change Tracking
    @Column(name = "changes_summary", columnDefinition = "TEXT")
    private String changesSummary;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_detail", columnDefinition = "jsonb")
    private Map<String, Object> changesDetail;
    
    // Creator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    // Status
    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;
    
    @Column(name = "is_auto_save")
    @Builder.Default
    private Boolean isAutoSave = false;
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
