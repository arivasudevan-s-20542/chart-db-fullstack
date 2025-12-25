package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a note/annotation on the diagram canvas.
 * Notes can be placed anywhere on the canvas to add documentation.
 */
@Entity
@Table(name = "notes", indexes = {
    @Index(name = "idx_notes_diagram", columnList = "diagram_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramNote {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "position_x", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionX = BigDecimal.ZERO;

    @Column(name = "position_y", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionY = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal width = new BigDecimal("200");

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal height = new BigDecimal("100");

    @Column(length = 20)
    private String color;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;

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
