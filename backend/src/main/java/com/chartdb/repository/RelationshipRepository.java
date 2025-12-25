package com.chartdb.repository;

import com.chartdb.model.Relationship;
import com.chartdb.model.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, String> {
    
    // Find all relationships in a diagram
    List<Relationship> findByDiagramId(String diagramId);
    
    // Find with eager loading of tables
    @Query("SELECT r FROM Relationship r " +
           "LEFT JOIN FETCH r.sourceTable " +
           "LEFT JOIN FETCH r.targetTable " +
           "LEFT JOIN FETCH r.sourceColumn " +
           "LEFT JOIN FETCH r.targetColumn " +
           "WHERE r.diagram.id = :diagramId")
    List<Relationship> findByDiagramIdWithTables(@Param("diagramId") String diagramId);
    
    // Find by id with tables
    @Query("SELECT r FROM Relationship r " +
           "LEFT JOIN FETCH r.sourceTable " +
           "LEFT JOIN FETCH r.targetTable " +
           "WHERE r.id = :relationshipId")
    Optional<Relationship> findByIdWithTables(@Param("relationshipId") String relationshipId);
    
    // Find relationships connected to a table
    @Query("SELECT r FROM Relationship r WHERE r.sourceTable.id = :tableId OR r.targetTable.id = :tableId")
    List<Relationship> findByTableId(@Param("tableId") String tableId);
    
    // Find outgoing relationships from a table
    List<Relationship> findBySourceTableId(String sourceTableId);
    
    // Find incoming relationships to a table
    List<Relationship> findByTargetTableId(String targetTableId);
    
    // Find relationships involving a column
    @Query("SELECT r FROM Relationship r WHERE r.sourceColumn.id = :columnId OR r.targetColumn.id = :columnId")
    List<Relationship> findByColumnId(@Param("columnId") String columnId);
    
    // Find specific relationship between two tables
    @Query("SELECT r FROM Relationship r WHERE " +
           "(r.sourceTable.id = :tableId1 AND r.targetTable.id = :tableId2) OR " +
           "(r.sourceTable.id = :tableId2 AND r.targetTable.id = :tableId1)")
    List<Relationship> findBetweenTables(
        @Param("tableId1") String tableId1,
        @Param("tableId2") String tableId2
    );
    
    // Check if relationship exists between columns
    boolean existsBySourceColumnIdAndTargetColumnId(String sourceColumnId, String targetColumnId);
    
    // Find by relationship type
    List<Relationship> findByDiagramIdAndRelationshipType(String diagramId, RelationshipType type);
    
    // Delete all relationships in diagram
    void deleteByDiagramId(String diagramId);
    
    // Delete relationships connected to a table
    @Modifying
    @Query("DELETE FROM Relationship r WHERE r.sourceTable.id = :tableId OR r.targetTable.id = :tableId")
    void deleteByTableId(@Param("tableId") String tableId);
    
    // Delete relationships involving a column
    @Modifying
    @Query("DELETE FROM Relationship r WHERE r.sourceColumn.id = :columnId OR r.targetColumn.id = :columnId")
    void deleteByColumnId(@Param("columnId") String columnId);
    
    // Count relationships in diagram
    long countByDiagramId(String diagramId);
    
    // Count by type
    long countByDiagramIdAndRelationshipType(String diagramId, RelationshipType type);
}
