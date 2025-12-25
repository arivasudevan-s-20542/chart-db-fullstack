package com.chartdb.repository;

import com.chartdb.model.ActiveCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveCollaboratorRepository extends JpaRepository<ActiveCollaborator, String> {
    
    // Find active collaborators in a diagram
    List<ActiveCollaborator> findByDiagramIdAndIsActiveTrue(String diagramId);
    
    // Find by session
    Optional<ActiveCollaborator> findBySessionId(String sessionId);
    
    // Find by websocket session
    Optional<ActiveCollaborator> findByWebsocketSessionId(String websocketSessionId);
    
    // Find by diagram and user
    Optional<ActiveCollaborator> findByDiagramIdAndUserId(String diagramId, String userId);
    
    // Find by diagram, user, and session
    Optional<ActiveCollaborator> findByDiagramIdAndUserIdAndSessionId(
        String diagramId, String userId, String sessionId
    );
    
    // Update cursor position
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET " +
           "c.cursorX = :x, c.cursorY = :y, " +
           "c.lastSeen = :timestamp, c.lastActivity = :timestamp " +
           "WHERE c.id = :collaboratorId")
    void updateCursorPosition(
        @Param("collaboratorId") String collaboratorId,
        @Param("x") BigDecimal x,
        @Param("y") BigDecimal y,
        @Param("timestamp") Instant timestamp
    );
    
    // Update selection
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET " +
           "c.selectedTable.id = :tableId, " +
           "c.lastActivity = :timestamp " +
           "WHERE c.id = :collaboratorId")
    void updateSelectedTable(
        @Param("collaboratorId") String collaboratorId,
        @Param("tableId") String tableId,
        @Param("timestamp") Instant timestamp
    );
    
    // Clear selection
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET " +
           "c.selectedTable = null, c.selectedColumn = null " +
           "WHERE c.id = :collaboratorId")
    void clearSelection(@Param("collaboratorId") String collaboratorId);
    
    // Mark as inactive
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET c.isActive = false WHERE c.id = :collaboratorId")
    void markInactive(@Param("collaboratorId") String collaboratorId);
    
    // Mark as idle
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET c.isIdle = :idle, c.status = :status WHERE c.id = :collaboratorId")
    void updateIdleStatus(
        @Param("collaboratorId") String collaboratorId,
        @Param("idle") boolean idle,
        @Param("status") String status
    );
    
    // Delete by session
    void deleteBySessionId(String sessionId);
    
    // Delete by websocket session
    void deleteByWebsocketSessionId(String websocketSessionId);
    
    // Delete inactive collaborators older than timestamp
    @Modifying
    @Query("DELETE FROM ActiveCollaborator c WHERE c.isActive = false AND c.lastSeen < :timestamp")
    void deleteInactiveBefore(@Param("timestamp") Instant timestamp);
    
    // Delete stale sessions (no activity for X seconds)
    @Modifying
    @Query("DELETE FROM ActiveCollaborator c WHERE c.lastSeen < :timestamp")
    void deleteStaleSessions(@Param("timestamp") Instant timestamp);
    
    // Count active collaborators in diagram
    long countByDiagramIdAndIsActiveTrue(String diagramId);
    
    // Check if user is currently in diagram
    boolean existsByDiagramIdAndUserIdAndIsActiveTrue(String diagramId, String userId);
    
    // Find all sessions for a user (across diagrams)
    List<ActiveCollaborator> findByUserIdAndIsActiveTrue(String userId);
    
    // Deactivate all sessions for a user
    @Modifying
    @Query("UPDATE ActiveCollaborator c SET c.isActive = false WHERE c.user.id = :userId")
    void deactivateAllForUser(@Param("userId") String userId);
}
