# 06 - Backend Repositories

## 🗄️ Spring Data JPA Repositories

### 1. UserRepository

```java
package com.chartdb.repository;

import com.chartdb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Find by email (for authentication)
    Optional<User> findByEmail(String email);
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Find active users
    List<User> findByIsActiveTrue();
    
    // Search users by email or username (for sharing)
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
    
    // Update last login
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId, @Param("timestamp") Instant timestamp);
    
    // Find by multiple IDs
    List<User> findByIdIn(List<String> ids);
}
```

---

### 2. DiagramRepository

```java
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
    @Query("UPDATE Diagram d SET d.status = 'ARCHIVED', d.archivedAt = :timestamp WHERE d.id = :diagramId")
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
```

---

### 3. TableRepository (DiagramTableRepository)

```java
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
```

---

### 4. ColumnRepository (TableColumnRepository)

```java
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
```

---

### 5. RelationshipRepository

```java
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
    
    // Update path points (for custom line routing)
    @Modifying
    @Query("UPDATE Relationship r SET r.pathPoints = :pathPoints WHERE r.id = :relationshipId")
    void updatePathPoints(
        @Param("relationshipId") String relationshipId,
        @Param("pathPoints") String pathPoints // JSON string
    );
    
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
```

---

### 6. DiagramPermissionRepository

```java
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
```

---

### 7. ActiveCollaboratorRepository

```java
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
```

---

### 8. DiagramVersionRepository

```java
package com.chartdb.repository;

import com.chartdb.model.DiagramVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, String> {
    
    // Find all versions for a diagram
    List<DiagramVersion> findByDiagramIdOrderByVersionNumberDesc(String diagramId);
    
    // Find versions with pagination
    Page<DiagramVersion> findByDiagramIdOrderByVersionNumberDesc(String diagramId, Pageable pageable);
    
    // Find current version
    Optional<DiagramVersion> findByDiagramIdAndIsCurrentTrue(String diagramId);
    
    // Find by version number
    Optional<DiagramVersion> findByDiagramIdAndVersionNumber(String diagramId, Integer versionNumber);
    
    // Get latest version number
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DiagramVersion v WHERE v.diagram.id = :diagramId")
    Integer getLatestVersionNumber(@Param("diagramId") String diagramId);
    
    // Clear current flag for all versions
    @Modifying
    @Query("UPDATE DiagramVersion v SET v.isCurrent = false WHERE v.diagram.id = :diagramId")
    void clearCurrentFlag(@Param("diagramId") String diagramId);
    
    // Set current version
    @Modifying
    @Query("UPDATE DiagramVersion v SET v.isCurrent = true WHERE v.id = :versionId")
    void setCurrentVersion(@Param("versionId") String versionId);
    
    // Delete old auto-save versions (keep last N)
    @Modifying
    @Query(value = "DELETE FROM diagram_versions WHERE diagram_id = :diagramId AND is_auto_save = true " +
                   "AND id NOT IN (SELECT id FROM diagram_versions WHERE diagram_id = :diagramId " +
                   "AND is_auto_save = true ORDER BY created_at DESC LIMIT :keepCount)", 
           nativeQuery = true)
    void deleteOldAutoSaves(@Param("diagramId") String diagramId, @Param("keepCount") int keepCount);
    
    // Count versions
    long countByDiagramId(String diagramId);
    
    // Find recent versions
    List<DiagramVersion> findTop10ByDiagramIdOrderByCreatedAtDesc(String diagramId);
}
```

---

### 9. AuditLogRepository

```java
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
```

---

### 10. TableLockRepository

```java
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
    
    // Find active lock (not expired)
    @Query("SELECT l FROM TableLock l WHERE l.table.id = :tableId AND l.expiresAt > :now")
    Optional<TableLock> findActiveLock(@Param("tableId") String tableId, @Param("now") Instant now);
    
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
    
    // Clean up expired locks
    @Modifying
    @Query("DELETE FROM TableLock l WHERE l.expiresAt < :now")
    int deleteExpiredLocks(@Param("now") Instant now);
    
    // Delete locks for tables in a diagram
    @Modifying
    @Query("DELETE FROM TableLock l WHERE l.table.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);
}
```

---

**← Previous:** `05-BACKEND-ENTITIES.md` | **Next:** `07-BACKEND-SERVICES.md` →
