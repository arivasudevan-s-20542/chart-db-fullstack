package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tables", indexes = {
    @Index(name = "idx_tables_diagram", columnList = "diagram_id"),
    @Index(name = "idx_tables_position", columnList = "diagram_id, position_x, position_y"),
    @Index(name = "idx_tables_name", columnList = "diagram_id, name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_table_name_per_diagram", columnNames = {"diagram_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DiagramTable extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    // Basic Info
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Position (CRITICAL - MUST ALWAYS BE SAVED)
    @Column(name = "position_x", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionX = BigDecimal.valueOf(100.00);
    
    @Column(name = "position_y", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionY = BigDecimal.valueOf(100.00);
    
    // Dimensions
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal width = BigDecimal.valueOf(200.00);
    
    @Column(precision = 10, scale = 2)
    private BigDecimal height;
    
    @Column(name = "min_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minWidth = BigDecimal.valueOf(150.00);
    
    @Column(name = "max_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxWidth = BigDecimal.valueOf(500.00);
    
    // Styling
    @Column(length = 7)
    @Builder.Default
    private String color = "#6366F1";
    
    @Column(name = "header_color", length = 7)
    private String headerColor;
    
    @Column(name = "border_color", length = 7)
    private String borderColor;
    
    @Column(name = "border_radius", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal borderRadius = BigDecimal.valueOf(8.0);
    
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal opacity = BigDecimal.ONE;
    
    // Flags
    @Column(name = "is_collapsed")
    @Builder.Default
    private Boolean isCollapsed = false;
    
    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;
    
    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;
    
    // Metadata
    @Column(name = "table_type", length = 50)
    @Builder.Default
    private String tableType = "table";
    
    @Column(name = "schema_name", length = 100)
    @Builder.Default
    private String schemaName = "public";
    
    // Ordering
    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Indexes stored as JSON string
    @Column(name = "indexes_json", columnDefinition = "TEXT")
    private String indexesJson;
    
    // View flags
    @Column(name = "is_view")
    @Builder.Default
    private Boolean isView = false;
    
    @Column(name = "is_materialized_view")
    @Builder.Default
    private Boolean isMaterializedView = false;
    
    // Relationships
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<TableColumn> columns = new ArrayList<>();
    
    @OneToMany(mappedBy = "sourceTable", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Relationship> outgoingRelationships = new ArrayList<>();
    
    @OneToMany(mappedBy = "targetTable", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Relationship> incomingRelationships = new ArrayList<>();
    
    // Helper methods
    public void addColumn(TableColumn column) {
        columns.add(column);
        column.setTable(this);
    }
    
    public void removeColumn(TableColumn column) {
        columns.remove(column);
        column.setTable(null);
    }
    
    // Update position method (CRITICAL)
    public void updatePosition(BigDecimal x, BigDecimal y) {
        this.positionX = x;
        this.positionY = y;
    }
    
    // Convenience method for double values
    public void updatePosition(double x, double y) {
        this.positionX = BigDecimal.valueOf(x);
        this.positionY = BigDecimal.valueOf(y);
    }
}
