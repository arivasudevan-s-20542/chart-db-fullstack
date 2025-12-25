package com.chartdb.repository;

import com.chartdb.model.TableLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableLockRepository extends JpaRepository<TableLock, String> {
    
    // Find lock by table
    Optional<TableLock> findByTableId(String tableId);
    
    // Find lock by table and user
    @Query("SELECT l FROM TableLock l WHERE l.table.id = :tableId AND l.user.id = :userId")
    Optional<TableLock> findByTableIdAndLockedByUserId(@Param("tableId") String tableId, @Param("userId") String userId);
    
    // Find active lock (not expired)
    @Query("SELECT l FROM TableLock l WHERE l.table.id = :tableId AND l.expiresAt > :now")
    Optional<TableLock> findByTableIdAndExpiresAtAfter(@Param("tableId") String tableId, @Param("now") Instant now);
    
    // Check if table is locked (not expired)
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM TableLock l WHERE l.table.id = :tableId AND l.expiresAt > :now")
    boolean existsByTableIdAndExpiresAtAfter(@Param("tableId") String tableId, @Param("now") Instant now);
    
    // Find locks by user
    List<TableLock> findByUserId(String userId);
    
    // Find locks in diagram
    @Query("SELECT l FROM TableLock l WHERE l.table.diagram.id = :diagramId")
    List<TableLock> findByDiagramId(@Param("diagramId") String diagramId);
    
    // Check if table is locked (by another user)
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM TableLock l WHERE l.table.id = :tableId AND l.user.id != :userId AND l.expiresAt > :now")
    boolean isLockedByOther(
        @Param("tableId") String tableId,
        @Param("userId") String userId,
        @Param("now") Instant now
    );
    
    // Extend lock
    @Modifying
    @Query("UPDATE TableLock l SET l.expiresAt = :newExpiry WHERE l.id = :lockId AND l.user.id = :userId")
    int extendLock(
        @Param("lockId") String lockId,
        @Param("userId") String userId,
        @Param("newExpiry") Instant newExpiry
    );
    
    // Delete by table
    void deleteByTableId(String tableId);
    
    // Delete by user
    void deleteByUserId(String userId);
    
    // Delete by diagram and user
    @Modifying
    @Query("DELETE FROM TableLock l WHERE l.table.diagram.id = :diagramId AND l.user.id = :userId")
    void deleteByDiagramIdAndLockedByUserId(@Param("diagramId") String diagramId, @Param("userId") String userId);
    
    // Clean up expired locks
    @Modifying
    @Query("DELETE FROM TableLock l WHERE l.expiresAt < :now")
    int deleteExpiredLocks(@Param("now") Instant now);
    
    // Delete locks for tables in a diagram
    @Modifying
    @Query("DELETE FROM TableLock l WHERE l.table.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);
}
