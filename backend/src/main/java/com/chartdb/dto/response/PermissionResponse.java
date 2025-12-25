package com.chartdb.dto.response;

import com.chartdb.model.enums.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    
    private String id;
    private String diagramId;
    
    // User info (may be null for pending invitations)
    private String userId;
    private String userEmail;
    private String userDisplayName;
    private String userAvatarUrl;
    
    // Invited email (for pending invitations)
    private String invitedEmail;
    
    // Permission details
    private PermissionLevel permissionLevel;
    private Boolean canEdit;
    private Boolean canComment;
    private Boolean canView;
    
    // Invitation status
    private String invitationStatus;
    private Instant invitedAt;
    private Instant acceptedAt;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
