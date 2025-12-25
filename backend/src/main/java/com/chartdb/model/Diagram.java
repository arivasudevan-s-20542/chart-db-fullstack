package com.chartdb.model;

import com.chartdb.model.enums.DiagramStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diagrams", indexes = {
    @Index(name = "idx_diagrams_owner", columnList = "owner_id"),
    @Index(name = "idx_diagrams_status", columnList = "status"),
    @Index(name = "idx_diagrams_slug", columnList = "public_slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Diagram extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String icon;
    
    @Column(length = 7)
    @Builder.Default
    private String color = "#6366F1";
    
    // Canvas State (CRITICAL)
    @Column(name = "canvas_zoom", precision = 5, scale = 3)
    @Builder.Default
    private BigDecimal canvasZoom = BigDecimal.valueOf(1.000);
    
    @Column(name = "canvas_offset_x", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    
    @Column(name = "canvas_offset_y", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;
    
    @Column(name = "canvas_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasWidth = BigDecimal.valueOf(5000.00);
    
    @Column(name = "canvas_height", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasHeight = BigDecimal.valueOf(5000.00);
    
    // Sharing
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    @Column(name = "public_slug", unique = true, length = 100)
    private String publicSlug;
    
    @Column(name = "allow_comments")
    @Builder.Default
    private Boolean allowComments = true;
    
    @Column(name = "allow_anonymous_view")
    @Builder.Default
    private Boolean allowAnonymousView = false;
    
    // Database Configuration
    @Column(name = "database_type", length = 50)
    @Builder.Default
    private String databaseType = "postgresql";
    
    @Column(name = "schema_name", length = 100)
    @Builder.Default
    private String schemaName = "public";
    
    // Metadata - Store tags as comma-separated string for better compatibility
    @Column(name = "tags", length = 2000)
    private String tags;
    
    @Column(length = 100)
    private String category;
    
    // Version Control
    @Column(name = "current_version_id", length = 36)
    private String currentVersionId;
    
    @Column(name = "version_number")
    @Builder.Default
    private Integer versionNumber = 1;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private DiagramStatus status = DiagramStatus.ACTIVE;
    
    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;
    
    // Statistics
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "export_count")
    @Builder.Default
    private Integer exportCount = 0;
    
    // Timestamps
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
    
    @Column(name = "archived_at")
    private Instant archivedAt;
    
    // Relationships
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagramTable> tables = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Relationship> relationships = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagramPermission> permissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ActiveCollaborator> activeCollaborators = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagramVersion> versions = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();
    
    // Helper methods
    public void addTable(DiagramTable table) {
        tables.add(table);
        table.setDiagram(this);
    }
    
    public void removeTable(DiagramTable table) {
        tables.remove(table);
        table.setDiagram(null);
    }
    
    public void addRelationship(Relationship relationship) {
        relationships.add(relationship);
        relationship.setDiagram(this);
    }
    
    public void removeRelationship(Relationship relationship) {
        relationships.remove(relationship);
        relationship.setDiagram(null);
    }
}
