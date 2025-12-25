package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "columns", indexes = {
    @Index(name = "idx_columns_table", columnList = "table_id"),
    @Index(name = "idx_columns_order", columnList = "table_id, order_index")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_column_name_per_table", columnNames = {"table_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TableColumn extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiagramTable table;
    
    // Basic Info
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Data Type
    @Column(name = "data_type", nullable = false, length = 100)
    private String dataType;
    
    @Column(name = "native_type", length = 100)
    private String nativeType;
    
    @Column
    private Integer length;
    
    @Column(name = "precision")
    private Integer precision;
    
    @Column
    private Integer scale;
    
    // Constraints
    @Column(name = "is_primary_key")
    @Builder.Default
    private Boolean isPrimaryKey = false;
    
    @Column(name = "is_foreign_key")
    @Builder.Default
    private Boolean isForeignKey = false;
    
    @Column(name = "is_nullable")
    @Builder.Default
    private Boolean isNullable = true;
    
    @Column(name = "is_unique")
    @Builder.Default
    private Boolean isUnique = false;
    
    @Column(name = "is_auto_increment")
    @Builder.Default
    private Boolean isAutoIncrement = false;
    
    @Column(name = "is_indexed")
    @Builder.Default
    private Boolean isIndexed = false;
    
    // Default Value
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;
    
    @Column(name = "default_expression", columnDefinition = "TEXT")
    private String defaultExpression;
    
    // Foreign Key Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_table_id")
    private DiagramTable fkTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_column_id")
    private TableColumn fkColumn;
    
    @Column(name = "fk_constraint_name", length = 255)
    private String fkConstraintName;
    
    @Column(name = "fk_on_delete", length = 20)
    @Builder.Default
    private String fkOnDelete = "NO ACTION";
    
    @Column(name = "fk_on_update", length = 20)
    @Builder.Default
    private String fkOnUpdate = "NO ACTION";
    
    // Check Constraint
    @Column(name = "check_constraint", columnDefinition = "TEXT")
    private String checkConstraint;
    
    // Enum Values - stored as comma-separated string
    @Column(name = "enum_values", length = 2000)
    private String enumValues;
    
    // Ordering
    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
    
    // Visibility
    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;
}
