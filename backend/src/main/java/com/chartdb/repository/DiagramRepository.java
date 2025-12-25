package com.chartdb.repository;

import com.chartdb.model.Diagram;
import com.chartdb.model.enums.DiagramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface DiagramRepository extends JpaRepository<Diagram, String> {
    
    // Find by owner
    List<Diagram> findByOwnerIdAndStatusNot(String ownerId, DiagramStatus status);
    
    Page<Diagram> findByOwnerIdAndStatusNot(String ownerId, DiagramStatus status, Pageable pageable);
    
    // Find by owner with status
    List<Diagram> findByOwnerIdAndStatus(String ownerId, DiagramStatus status);
    
    // Find public diagrams
    Page<Diagram> findByIsPublicTrueAndStatus(DiagramStatus status, Pageable pageable);
    
    // Find by public slug
    Optional<Diagram> findByPublicSlug(String publicSlug);
    
    // Find templates
    List<Diagram> findByIsTemplateTrueAndStatus(DiagramStatus status);
    
    // Search diagrams by name
    @Query("SELECT d FROM Diagram d WHERE " +
           "d.owner.id = :ownerId AND " +
           "d.status = :status AND " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Diagram> searchByOwnerAndName(
        @Param("ownerId") String ownerId,
        @Param("status") DiagramStatus status,
        @Param("query") String query
    );
    
    // Find diagrams user has access to (via permissions)
    @Query("SELECT DISTINCT d FROM Diagram d " +
           "LEFT JOIN DiagramPermission p ON p.diagram = d " +
           "WHERE (d.owner.id = :userId OR p.user.id = :userId) " +
           "AND d.status = :status")
    Page<Diagram> findAccessibleByUser(
        @Param("userId") String userId,
        @Param("status") DiagramStatus status,
        Pageable pageable
    );
    
    // Update canvas state
    @Modifying
    @Query("UPDATE Diagram d SET " +
           "d.canvasZoom = :zoom, " +
           "d.canvasOffsetX = :offsetX, " +
           "d.canvasOffsetY = :offsetY, " +
           "d.updatedAt = :timestamp " +
           "WHERE d.id = :diagramId")
    void updateCanvasState(
        @Param("diagramId") String diagramId,
        @Param("zoom") BigDecimal zoom,
        @Param("offsetX") BigDecimal offsetX,
        @Param("offsetY") BigDecimal offsetY,
        @Param("timestamp") Instant timestamp
    );
    
    // Update last accessed
    @Modifying
    @Query("UPDATE Diagram d SET d.lastAccessedAt = :timestamp WHERE d.id = :diagramId")
    void updateLastAccessed(@Param("diagramId") String diagramId, @Param("timestamp") Instant timestamp);
    
    // Increment view count
    @Modifying
    @Query("UPDATE Diagram d SET d.viewCount = d.viewCount + 1 WHERE d.id = :diagramId")
    void incrementViewCount(@Param("diagramId") String diagramId);
    
    // Increment export count
    @Modifying
    @Query("UPDATE Diagram d SET d.exportCount = d.exportCount + 1 WHERE d.id = :diagramId")
    void incrementExportCount(@Param("diagramId") String diagramId);
    
    // Archive diagram
    @Modifying
    @Query("UPDATE Diagram d SET d.status = com.chartdb.model.enums.DiagramStatus.ARCHIVED, d.archivedAt = :timestamp WHERE d.id = :diagramId")
    void archiveDiagram(@Param("diagramId") String diagramId, @Param("timestamp") Instant timestamp);
    
    // Count diagrams by owner
    long countByOwnerIdAndStatus(String ownerId, DiagramStatus status);
    
    // Find recently accessed
    @Query("SELECT d FROM Diagram d WHERE d.owner.id = :ownerId AND d.status = :status " +
           "ORDER BY d.lastAccessedAt DESC")
    List<Diagram> findRecentlyAccessed(
        @Param("ownerId") String ownerId,
        @Param("status") DiagramStatus status,
        Pageable pageable
    );
    
    // Check if slug exists
    boolean existsByPublicSlug(String publicSlug);
}
