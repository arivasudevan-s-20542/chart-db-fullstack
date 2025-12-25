package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a dependency between database objects in a diagram.
 * For example, a view depending on a table, or a function depending on another function.
 */
@Entity
@Table(name = "dependencies", indexes = {
    @Index(name = "idx_dependencies_diagram", columnList = "diagram_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramDependency {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;

    @Column(name = "source_table_id", length = 36)
    private String sourceTableId;

    @Column(name = "target_table_id", length = 36)
    private String targetTableId;

    @Column(name = "dependency_type", length = 50)
    private String dependencyType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
