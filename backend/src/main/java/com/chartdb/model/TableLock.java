package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "table_locks", indexes = {
    @Index(name = "idx_locks_table", columnList = "table_id"),
    @Index(name = "idx_locks_expiry", columnList = "expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_table_lock", columnNames = {"table_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableLock {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiagramTable table;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "lock_type", length = 20)
    @Builder.Default
    private String lockType = "edit";
    
    @Column(name = "acquired_at")
    @Builder.Default
    private Instant acquiredAt = Instant.now();
    
    @Column(name = "expires_at")
    @Builder.Default
    private Instant expiresAt = Instant.now().plusSeconds(30);
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public void extendLock(int seconds) {
        this.expiresAt = Instant.now().plusSeconds(seconds);
    }
}
