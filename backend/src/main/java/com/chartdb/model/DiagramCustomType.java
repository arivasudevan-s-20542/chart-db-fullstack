package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a custom data type in a diagram.
 * Custom types include enums, composite types, domains, etc.
 */
@Entity
@Table(name = "custom_types", indexes = {
    @Index(name = "idx_custom_types_diagram", columnList = "diagram_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramCustomType {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String kind; // enum, composite, domain, etc.

    @Column(name = "schema_name", length = 255)
    private String schemaName;

    @Column(columnDefinition = "TEXT")
    private String values; // JSON array for enum values

    @Column(columnDefinition = "TEXT")
    private String attributes; // JSON for composite type attributes

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
