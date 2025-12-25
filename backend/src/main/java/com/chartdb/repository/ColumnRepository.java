package com.chartdb.repository;

import com.chartdb.model.TableColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColumnRepository extends JpaRepository<TableColumn, String> {
    
    // Find all columns in a table (ordered)
    List<TableColumn> findByTableIdOrderByOrderIndexAsc(String tableId);
    
    // Find by table id
    List<TableColumn> findByTableId(String tableId);
    
    // Find by name in table
    Optional<TableColumn> findByTableIdAndName(String tableId, String name);
    
    // Check if name exists
    boolean existsByTableIdAndName(String tableId, String name);
    
    // Check if name exists (excluding specific column)
    boolean existsByTableIdAndNameAndIdNot(String tableId, String name, String excludeId);
    
    // Find primary keys
    List<TableColumn> findByTableIdAndIsPrimaryKeyTrue(String tableId);
    
    // Find foreign keys
    List<TableColumn> findByTableIdAndIsForeignKeyTrue(String tableId);
    
    // Find columns referencing a specific table
    List<TableColumn> findByFkTableId(String fkTableId);
    
    // Find columns referencing a specific column
    List<TableColumn> findByFkColumnId(String fkColumnId);
    
    // Get max order index for new column placement
    @Query("SELECT COALESCE(MAX(c.orderIndex), -1) FROM TableColumn c WHERE c.table.id = :tableId")
    Integer getMaxOrderIndex(@Param("tableId") String tableId);
    
    // Reorder columns (shift indices)
    @Modifying
    @Query("UPDATE TableColumn c SET c.orderIndex = c.orderIndex + 1 " +
           "WHERE c.table.id = :tableId AND c.orderIndex >= :startIndex")
    void shiftOrderIndicesUp(
        @Param("tableId") String tableId,
        @Param("startIndex") Integer startIndex
    );
    
    @Modifying
    @Query("UPDATE TableColumn c SET c.orderIndex = c.orderIndex - 1 " +
           "WHERE c.table.id = :tableId AND c.orderIndex > :removedIndex")
    void shiftOrderIndicesDown(
        @Param("tableId") String tableId,
        @Param("removedIndex") Integer removedIndex
    );
    
    // Update order index
    @Modifying
    @Query("UPDATE TableColumn c SET c.orderIndex = :orderIndex WHERE c.id = :columnId")
    void updateOrderIndex(@Param("columnId") String columnId, @Param("orderIndex") Integer orderIndex);
    
    // Delete all columns in table
    void deleteByTableId(String tableId);
    
    // Count columns in table
    long countByTableId(String tableId);
    
    // Find all columns in a diagram (via tables)
    @Query("SELECT c FROM TableColumn c WHERE c.table.diagram.id = :diagramId ORDER BY c.table.id, c.orderIndex")
    List<TableColumn> findByDiagramId(@Param("diagramId") String diagramId);
    
    // Clear foreign key references when a table is deleted
    @Modifying
    @Query("UPDATE TableColumn c SET c.fkTable = null, c.fkColumn = null, c.isForeignKey = false " +
           "WHERE c.fkTable.id = :tableId")
    void clearForeignKeyReferences(@Param("tableId") String tableId);
}
