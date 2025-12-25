package com.chartdb.repository;

import com.chartdb.model.AuditLog;
import com.chartdb.model.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    // Find by diagram
    Page<AuditLog> findByDiagramIdOrderByCreatedAtDesc(String diagramId, Pageable pageable);
    
    // Find by user
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find by action type
    Page<AuditLog> findByActionTypeOrderByCreatedAtDesc(ActionType actionType, Pageable pageable);
    
    // Find by entity
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    
    // Find by diagram and action type
    List<AuditLog> findByDiagramIdAndActionTypeOrderByCreatedAtDesc(String diagramId, ActionType actionType);
    
    // Find by time range
    @Query("SELECT a FROM AuditLog a WHERE a.diagram.id = :diagramId " +
           "AND a.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByDiagramIdAndTimeRange(
        @Param("diagramId") String diagramId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
    
    // Count by diagram and action type
    long countByDiagramIdAndActionType(String diagramId, ActionType actionType);
    
    // Delete old logs
    void deleteByCreatedAtBefore(Instant timestamp);
    
    // Recent activity
    List<AuditLog> findTop50ByDiagramIdOrderByCreatedAtDesc(String diagramId);
}
