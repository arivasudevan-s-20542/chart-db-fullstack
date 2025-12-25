package com.chartdb.model;

import com.chartdb.model.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "relationships", indexes = {
    @Index(name = "idx_relationships_diagram", columnList = "diagram_id"),
    @Index(name = "idx_relationships_source", columnList = "source_table_id"),
    @Index(name = "idx_relationships_target", columnList = "target_table_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Relationship extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    // Source and Target
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_table_id", nullable = false)
    private DiagramTable sourceTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_table_id", nullable = false)
    private DiagramTable targetTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_column_id")
    private TableColumn sourceColumn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_column_id")
    private TableColumn targetColumn;
    
    // Relationship Type
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;
    
    @Column(name = "cardinality_source", length = 10)
    @Builder.Default
    private String cardinalitySource = "1";
    
    @Column(name = "cardinality_target", length = 10)
    @Builder.Default
    private String cardinalityTarget = "*";
    
    // Naming
    @Column(length = 255)
    private String name;
    
    @Column(name = "constraint_name", length = 255)
    private String constraintName;
    
    // Visual/Path
    @Column(name = "path_type", length = 20)
    @Builder.Default
    private String pathType = "auto";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_points", columnDefinition = "jsonb")
    private List<Map<String, Object>> pathPoints;
    
    @Column(name = "source_handle", length = 20)
    @Builder.Default
    private String sourceHandle = "right";
    
    @Column(name = "target_handle", length = 20)
    @Builder.Default
    private String targetHandle = "left";
    
    // Styling
    @Column(name = "line_color", length = 7)
    @Builder.Default
    private String lineColor = "#94A3B8";
    
    @Column(name = "line_width", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal lineWidth = BigDecimal.valueOf(2.0);
    
    @Column(name = "line_style", length = 20)
    @Builder.Default
    private String lineStyle = "solid";
    
    // Labels
    @Column(name = "label_source", length = 100)
    private String labelSource;
    
    @Column(name = "label_target", length = 100)
    private String labelTarget;
    
    @Column(name = "show_labels")
    @Builder.Default
    private Boolean showLabels = true;
    
    // Referential Actions
    @Column(name = "on_delete", length = 20)
    @Builder.Default
    private String onDelete = "NO ACTION";
    
    @Column(name = "on_update", length = 20)
    @Builder.Default
    private String onUpdate = "NO ACTION";
    
    // Flags
    @Column(name = "is_identifying")
    @Builder.Default
    private Boolean isIdentifying = false;
    
    @Column(name = "is_virtual")
    @Builder.Default
    private Boolean isVirtual = false;
    
    // Ordering
    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;
}
