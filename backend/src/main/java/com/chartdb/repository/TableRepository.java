package com.chartdb.repository;

import com.chartdb.model.DiagramTable;
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
public interface TableRepository extends JpaRepository<DiagramTable, String> {
    
    // Find all tables in a diagram
    List<DiagramTable> findByDiagramIdOrderBySortOrderAsc(String diagramId);
    
    // Find all tables ordered by creation date
    List<DiagramTable> findByDiagramIdOrderByCreatedAtAsc(String diagramId);
    
    // Find by diagram (eager load columns for full diagram fetch)
    @Query("SELECT DISTINCT t FROM DiagramTable t " +
           "LEFT JOIN FETCH t.columns " +
           "WHERE t.diagram.id = :diagramId " +
           "ORDER BY t.sortOrder ASC")
    List<DiagramTable> findByDiagramIdWithColumns(@Param("diagramId") String diagramId);
    
    // Find by id with columns
    @Query("SELECT t FROM DiagramTable t " +
           "LEFT JOIN FETCH t.columns " +
           "WHERE t.id = :tableId")
    Optional<DiagramTable> findByIdWithColumns(@Param("tableId") String tableId);
    
    // Find by name in diagram
    Optional<DiagramTable> findByDiagramIdAndName(String diagramId, String name);
    
    // Check if name exists in diagram
    boolean existsByDiagramIdAndName(String diagramId, String name);
    
    // Check if name exists in diagram (excluding a specific table)
    boolean existsByDiagramIdAndNameAndIdNot(String diagramId, String name, String excludeId);
    
    // Update position (CRITICAL METHOD)
    @Modifying
    @Query("UPDATE DiagramTable t SET " +
           "t.positionX = :x, " +
           "t.positionY = :y, " +
           "t.updatedAt = :timestamp " +
           "WHERE t.id = :tableId")
    void updatePosition(
        @Param("tableId") String tableId,
        @Param("x") BigDecimal x,
        @Param("y") BigDecimal y,
        @Param("timestamp") Instant timestamp
    );
    
    // Batch update positions (for multi-select drag)
    @Modifying
    @Query("UPDATE DiagramTable t SET " +
           "t.positionX = t.positionX + :deltaX, " +
           "t.positionY = t.positionY + :deltaY, " +
           "t.updatedAt = :timestamp " +
           "WHERE t.id IN :tableIds")
    void updatePositionsByDelta(
        @Param("tableIds") List<String> tableIds,
        @Param("deltaX") BigDecimal deltaX,
        @Param("deltaY") BigDecimal deltaY,
        @Param("timestamp") Instant timestamp
    );
    
    // Update dimensions
    @Modifying
    @Query("UPDATE DiagramTable t SET " +
           "t.width = :width, " +
           "t.height = :height, " +
           "t.updatedAt = :timestamp " +
           "WHERE t.id = :tableId")
    void updateDimensions(
        @Param("tableId") String tableId,
        @Param("width") BigDecimal width,
        @Param("height") BigDecimal height,
        @Param("timestamp") Instant timestamp
    );
    
    // Toggle collapsed state
    @Modifying
    @Query("UPDATE DiagramTable t SET t.isCollapsed = :collapsed, t.updatedAt = :timestamp WHERE t.id = :tableId")
    void updateCollapsed(
        @Param("tableId") String tableId,
        @Param("collapsed") Boolean collapsed,
        @Param("timestamp") Instant timestamp
    );
    
    // Update color
    @Modifying
    @Query("UPDATE DiagramTable t SET t.color = :color, t.updatedAt = :timestamp WHERE t.id = :tableId")
    void updateColor(
        @Param("tableId") String tableId,
        @Param("color") String color,
        @Param("timestamp") Instant timestamp
    );
    
    // Delete all tables in diagram
    void deleteByDiagramId(String diagramId);
    
    // Count tables in diagram
    long countByDiagramId(String diagramId);
    
    // Get max sort order for new table placement
    @Query("SELECT COALESCE(MAX(t.sortOrder), 0) FROM DiagramTable t WHERE t.diagram.id = :diagramId")
    Integer getMaxSortOrder(@Param("diagramId") String diagramId);
    
    // Get max z-index for bringing table to front
    @Query("SELECT COALESCE(MAX(t.zIndex), 0) FROM DiagramTable t WHERE t.diagram.id = :diagramId")
    Integer getMaxZIndex(@Param("diagramId") String diagramId);
    
    // Update z-index (bring to front)
    @Modifying
    @Query("UPDATE DiagramTable t SET t.zIndex = :zIndex WHERE t.id = :tableId")
    void updateZIndex(@Param("tableId") String tableId, @Param("zIndex") Integer zIndex);
    
    // Find hidden tables
    List<DiagramTable> findByDiagramIdAndIsHiddenTrue(String diagramId);
    
    // Find visible tables only
    List<DiagramTable> findByDiagramIdAndIsHiddenFalse(String diagramId);
}
