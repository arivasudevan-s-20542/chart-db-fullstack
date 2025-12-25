package com.chartdb.repository;

import com.chartdb.model.DiagramPermission;
import com.chartdb.model.enums.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagramPermissionRepository extends JpaRepository<DiagramPermission, String> {
    
    // Find all permissions for a diagram
    List<DiagramPermission> findByDiagramId(String diagramId);
    
    // Find permission by diagram and user
    Optional<DiagramPermission> findByDiagramIdAndUserId(String diagramId, String userId);
    
    // Find permission by diagram and email (for pending invitations)
    Optional<DiagramPermission> findByDiagramIdAndInvitedEmail(String diagramId, String email);
    
    // Find all permissions for a user
    List<DiagramPermission> findByUserId(String userId);
    
    // Find pending invitations for an email
    List<DiagramPermission> findByInvitedEmailAndInvitationStatus(String email, String status);
    
    // Check if user has any permission
    boolean existsByDiagramIdAndUserId(String diagramId, String userId);
    
    // Check if user can edit
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM DiagramPermission p " +
           "WHERE p.diagram.id = :diagramId AND p.user.id = :userId AND p.canEdit = true")
    boolean canUserEdit(@Param("diagramId") String diagramId, @Param("userId") String userId);
    
    // Check if user can view
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM DiagramPermission p " +
           "WHERE p.diagram.id = :diagramId AND p.user.id = :userId AND p.canView = true")
    boolean canUserView(@Param("diagramId") String diagramId, @Param("userId") String userId);
    
    // Find editors
    List<DiagramPermission> findByDiagramIdAndPermissionLevel(String diagramId, PermissionLevel level);
    
    // Update permission level
    @Modifying
    @Query("UPDATE DiagramPermission p SET p.permissionLevel = :level, " +
           "p.canEdit = :canEdit, p.canComment = :canComment " +
           "WHERE p.id = :permissionId")
    void updatePermissionLevel(
        @Param("permissionId") String permissionId,
        @Param("level") PermissionLevel level,
        @Param("canEdit") boolean canEdit,
        @Param("canComment") boolean canComment
    );
    
    // Delete all permissions for a diagram
    void deleteByDiagramId(String diagramId);
    
    // Delete permission by user and diagram
    void deleteByDiagramIdAndUserId(String diagramId, String userId);
    
    // Count collaborators
    long countByDiagramId(String diagramId);
    
    // Find with user details
    @Query("SELECT p FROM DiagramPermission p LEFT JOIN FETCH p.user WHERE p.diagram.id = :diagramId")
    List<DiagramPermission> findByDiagramIdWithUser(@Param("diagramId") String diagramId);
}
