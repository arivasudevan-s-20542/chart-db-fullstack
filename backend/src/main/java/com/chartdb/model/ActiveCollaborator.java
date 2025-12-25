package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "active_collaborators", indexes = {
    @Index(name = "idx_collaborators_diagram", columnList = "diagram_id"),
    @Index(name = "idx_collaborators_session", columnList = "session_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_session", columnNames = {"diagram_id", "user_id", "session_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ActiveCollaborator extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Session Info
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    @Column(name = "websocket_session_id", length = 100)
    private String websocketSessionId;
    
    // Cursor Position
    @Column(name = "cursor_x", precision = 10, scale = 2)
    private BigDecimal cursorX;
    
    @Column(name = "cursor_y", precision = 10, scale = 2)
    private BigDecimal cursorY;
    
    @Column(name = "cursor_color", length = 7)
    private String cursorColor;
    
    // Denormalized User Info (for performance)
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "user_avatar", length = 500)
    private String userAvatar;
    
    // Selection
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_table_id")
    private DiagramTable selectedTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_column_id")
    private TableColumn selectedColumn;
    
    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_idle")
    @Builder.Default
    private Boolean isIdle = false;
    
    @Column(length = 20)
    @Builder.Default
    private String status = "active";
    
    // Device Info
    @Column(name = "device_type", length = 20)
    private String deviceType;
    
    @Column(length = 50)
    private String browser;
    
    // Timestamps
    @Column(name = "connected_at")
    @Builder.Default
    private Instant connectedAt = Instant.now();
    
    @Column(name = "last_seen")
    @Builder.Default
    private Instant lastSeen = Instant.now();
    
    @Column(name = "last_activity")
    @Builder.Default
    private Instant lastActivity = Instant.now();
    
    // Update cursor position
    public void updateCursor(BigDecimal x, BigDecimal y) {
        this.cursorX = x;
        this.cursorY = y;
        this.lastSeen = Instant.now();
        this.lastActivity = Instant.now();
    }
}
