package com.chartdb.model;

import com.chartdb.model.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "diagram_permissions", indexes = {
    @Index(name = "idx_permissions_diagram", columnList = "diagram_id"),
    @Index(name = "idx_permissions_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_user_diagram", columnNames = {"diagram_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DiagramPermission extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "invited_email", length = 255)
    private String invitedEmail;
    
    // Permission Level
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.VIEWER;
    
    // Granular Permissions
    @Column(name = "can_view")
    @Builder.Default
    private Boolean canView = true;
    
    @Column(name = "can_edit")
    @Builder.Default
    private Boolean canEdit = false;
    
    @Column(name = "can_comment")
    @Builder.Default
    private Boolean canComment = false;
    
    @Column(name = "can_share")
    @Builder.Default
    private Boolean canShare = false;
    
    @Column(name = "can_export")
    @Builder.Default
    private Boolean canExport = true;
    
    @Column(name = "can_delete")
    @Builder.Default
    private Boolean canDelete = false;
    
    @Column(name = "can_manage_permissions")
    @Builder.Default
    private Boolean canManagePermissions = false;
    
    // Invitation Status
    @Column(name = "invitation_status", length = 20)
    @Builder.Default
    private String invitationStatus = "accepted";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;
    
    @Column(name = "invited_at")
    private Instant invitedAt;
    
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    
    // Expiration
    @Column(name = "expires_at")
    private Instant expiresAt;
}
