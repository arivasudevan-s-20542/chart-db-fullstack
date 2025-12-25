# 05 - Backend JPA Entities

## 🗂️ JPA Entity Classes

### Base Entity (Auditing)

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
```

---

### 1. User Entity

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "cursor_color", length = 7)
    @Builder.Default
    private String cursorColor = "#3B82F6";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> preferences = Map.of();
    
    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";
    
    @Column(length = 10)
    @Builder.Default
    private String locale = "en-US";
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    // Helper method to get display name or username
    public String getEffectiveDisplayName() {
        return displayName != null ? displayName : username;
    }
}
```

---

### 2. Diagram Entity

```java
package com.chartdb.model;

import com.chartdb.model.enums.DiagramStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diagrams", indexes = {
    @Index(name = "idx_diagrams_owner", columnList = "owner_id"),
    @Index(name = "idx_diagrams_status", columnList = "status"),
    @Index(name = "idx_diagrams_slug", columnList = "public_slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagram extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String icon;
    
    @Column(length = 7)
    @Builder.Default
    private String color = "#6366F1";
    
    // Canvas State (CRITICAL)
    @Column(name = "canvas_zoom", precision = 5, scale = 3)
    @Builder.Default
    private BigDecimal canvasZoom = BigDecimal.valueOf(1.000);
    
    @Column(name = "canvas_offset_x", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    
    @Column(name = "canvas_offset_y", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;
    
    @Column(name = "canvas_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasWidth = BigDecimal.valueOf(5000.00);
    
    @Column(name = "canvas_height", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal canvasHeight = BigDecimal.valueOf(5000.00);
    
    // Sharing
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    @Column(name = "public_slug", unique = true, length = 100)
    private String publicSlug;
    
    @Column(name = "allow_comments")
    @Builder.Default
    private Boolean allowComments = true;
    
    @Column(name = "allow_anonymous_view")
    @Builder.Default
    private Boolean allowAnonymousView = false;
    
    // Database Configuration
    @Column(name = "database_type", length = 50)
    @Builder.Default
    private String databaseType = "postgresql";
    
    @Column(name = "schema_name", length = 100)
    @Builder.Default
    private String schemaName = "public";
    
    // Metadata
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(255)[]")
    private String[] tags;
    
    @Column(length = 100)
    private String category;
    
    // Version Control
    @Column(name = "current_version_id", length = 36)
    private String currentVersionId;
    
    @Column(name = "version_number")
    @Builder.Default
    private Integer versionNumber = 1;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private DiagramStatus status = DiagramStatus.ACTIVE;
    
    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;
    
    // Statistics
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "export_count")
    @Builder.Default
    private Integer exportCount = 0;
    
    // Timestamps
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
    
    @Column(name = "archived_at")
    private Instant archivedAt;
    
    // Relationships
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagramTable> tables = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Relationship> relationships = new ArrayList<>();
    
    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagramPermission> permissions = new ArrayList<>();
    
    // Helper methods
    public void addTable(DiagramTable table) {
        tables.add(table);
        table.setDiagram(this);
    }
    
    public void removeTable(DiagramTable table) {
        tables.remove(table);
        table.setDiagram(null);
    }
    
    public void addRelationship(Relationship relationship) {
        relationships.add(relationship);
        relationship.setDiagram(this);
    }
    
    public void removeRelationship(Relationship relationship) {
        relationships.remove(relationship);
        relationship.setDiagram(null);
    }
}
```

---

### 3. Table Entity (DiagramTable)

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tables", indexes = {
    @Index(name = "idx_tables_diagram", columnList = "diagram_id"),
    @Index(name = "idx_tables_position", columnList = "diagram_id, position_x, position_y"),
    @Index(name = "idx_tables_name", columnList = "diagram_id, name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_table_name_per_diagram", columnNames = {"diagram_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramTable extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    // Basic Info
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Position (CRITICAL - MUST ALWAYS BE SAVED)
    @Column(name = "position_x", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionX = BigDecimal.valueOf(100.00);
    
    @Column(name = "position_y", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal positionY = BigDecimal.valueOf(100.00);
    
    // Dimensions
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal width = BigDecimal.valueOf(200.00);
    
    @Column(precision = 10, scale = 2)
    private BigDecimal height;
    
    @Column(name = "min_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minWidth = BigDecimal.valueOf(150.00);
    
    @Column(name = "max_width", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxWidth = BigDecimal.valueOf(500.00);
    
    // Styling
    @Column(length = 7)
    @Builder.Default
    private String color = "#6366F1";
    
    @Column(name = "header_color", length = 7)
    private String headerColor;
    
    @Column(name = "border_color", length = 7)
    private String borderColor;
    
    @Column(name = "border_radius", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal borderRadius = BigDecimal.valueOf(8.0);
    
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal opacity = BigDecimal.ONE;
    
    // Flags
    @Column(name = "is_collapsed")
    @Builder.Default
    private Boolean isCollapsed = false;
    
    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;
    
    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;
    
    // Metadata
    @Column(name = "table_type", length = 50)
    @Builder.Default
    private String tableType = "table";
    
    @Column(name = "schema_name", length = 100)
    @Builder.Default
    private String schemaName = "public";
    
    // Ordering
    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Relationships
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<TableColumn> columns = new ArrayList<>();
    
    @OneToMany(mappedBy = "sourceTable", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Relationship> outgoingRelationships = new ArrayList<>();
    
    @OneToMany(mappedBy = "targetTable", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Relationship> incomingRelationships = new ArrayList<>();
    
    // Helper methods
    public void addColumn(TableColumn column) {
        columns.add(column);
        column.setTable(this);
    }
    
    public void removeColumn(TableColumn column) {
        columns.remove(column);
        column.setTable(null);
    }
    
    // Update position method (CRITICAL)
    public void updatePosition(BigDecimal x, BigDecimal y) {
        this.positionX = x;
        this.positionY = y;
    }
    
    // Convenience method for double values
    public void updatePosition(double x, double y) {
        this.positionX = BigDecimal.valueOf(x);
        this.positionY = BigDecimal.valueOf(y);
    }
}
```

---

### 4. Column Entity (TableColumn)

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "columns", indexes = {
    @Index(name = "idx_columns_table", columnList = "table_id"),
    @Index(name = "idx_columns_order", columnList = "table_id, order_index")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_column_name_per_table", columnNames = {"table_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableColumn extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiagramTable table;
    
    // Basic Info
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Data Type
    @Column(name = "data_type", nullable = false, length = 100)
    private String dataType;
    
    @Column(name = "native_type", length = 100)
    private String nativeType;
    
    @Column
    private Integer length;
    
    @Column(name = "precision")
    private Integer precision;
    
    @Column
    private Integer scale;
    
    // Constraints
    @Column(name = "is_primary_key")
    @Builder.Default
    private Boolean isPrimaryKey = false;
    
    @Column(name = "is_foreign_key")
    @Builder.Default
    private Boolean isForeignKey = false;
    
    @Column(name = "is_nullable")
    @Builder.Default
    private Boolean isNullable = true;
    
    @Column(name = "is_unique")
    @Builder.Default
    private Boolean isUnique = false;
    
    @Column(name = "is_auto_increment")
    @Builder.Default
    private Boolean isAutoIncrement = false;
    
    @Column(name = "is_indexed")
    @Builder.Default
    private Boolean isIndexed = false;
    
    // Default Value
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;
    
    @Column(name = "default_expression", columnDefinition = "TEXT")
    private String defaultExpression;
    
    // Foreign Key Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_table_id")
    private DiagramTable fkTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_column_id")
    private TableColumn fkColumn;
    
    @Column(name = "fk_constraint_name", length = 255)
    private String fkConstraintName;
    
    @Column(name = "fk_on_delete", length = 20)
    @Builder.Default
    private String fkOnDelete = "NO ACTION";
    
    @Column(name = "fk_on_update", length = 20)
    @Builder.Default
    private String fkOnUpdate = "NO ACTION";
    
    // Check Constraint
    @Column(name = "check_constraint", columnDefinition = "TEXT")
    private String checkConstraint;
    
    // Enum Values
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enum_values", columnDefinition = "text[]")
    private String[] enumValues;
    
    // Ordering
    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
    
    // Visibility
    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;
}
```

---

### 5. Relationship Entity

```java
package com.chartdb.model;

import com.chartdb.model.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "relationships", indexes = {
    @Index(name = "idx_relationships_diagram", columnList = "diagram_id"),
    @Index(name = "idx_relationships_source", columnList = "source_table_id"),
    @Index(name = "idx_relationships_target", columnList = "target_table_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Relationship extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    // Source and Target
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_table_id", nullable = false)
    private DiagramTable sourceTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_table_id", nullable = false)
    private DiagramTable targetTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_column_id")
    private TableColumn sourceColumn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_column_id")
    private TableColumn targetColumn;
    
    // Relationship Type
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;
    
    @Column(name = "cardinality_source", length = 10)
    @Builder.Default
    private String cardinalitySource = "1";
    
    @Column(name = "cardinality_target", length = 10)
    @Builder.Default
    private String cardinalityTarget = "*";
    
    // Naming
    @Column(length = 255)
    private String name;
    
    @Column(name = "constraint_name", length = 255)
    private String constraintName;
    
    // Visual/Path
    @Column(name = "path_type", length = 20)
    @Builder.Default
    private String pathType = "auto";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_points", columnDefinition = "jsonb")
    private List<Map<String, Double>> pathPoints;
    
    @Column(name = "source_handle", length = 20)
    @Builder.Default
    private String sourceHandle = "right";
    
    @Column(name = "target_handle", length = 20)
    @Builder.Default
    private String targetHandle = "left";
    
    // Styling
    @Column(name = "line_color", length = 7)
    @Builder.Default
    private String lineColor = "#94A3B8";
    
    @Column(name = "line_width", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal lineWidth = BigDecimal.valueOf(2.0);
    
    @Column(name = "line_style", length = 20)
    @Builder.Default
    private String lineStyle = "solid";
    
    // Labels
    @Column(name = "label_source", length = 100)
    private String labelSource;
    
    @Column(name = "label_target", length = 100)
    private String labelTarget;
    
    @Column(name = "show_labels")
    @Builder.Default
    private Boolean showLabels = true;
    
    // Referential Actions
    @Column(name = "on_delete", length = 20)
    @Builder.Default
    private String onDelete = "NO ACTION";
    
    @Column(name = "on_update", length = 20)
    @Builder.Default
    private String onUpdate = "NO ACTION";
    
    // Flags
    @Column(name = "is_identifying")
    @Builder.Default
    private Boolean isIdentifying = false;
    
    @Column(name = "is_virtual")
    @Builder.Default
    private Boolean isVirtual = false;
    
    // Ordering
    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;
}
```

---

### 6. DiagramPermission Entity

```java
package com.chartdb.model;

import com.chartdb.model.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "diagram_permissions", indexes = {
    @Index(name = "idx_permissions_diagram", columnList = "diagram_id"),
    @Index(name = "idx_permissions_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_user_diagram", columnNames = {"diagram_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramPermission extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "invited_email", length = 255)
    private String invitedEmail;
    
    // Permission Level
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.VIEWER;
    
    // Granular Permissions
    @Column(name = "can_view")
    @Builder.Default
    private Boolean canView = true;
    
    @Column(name = "can_edit")
    @Builder.Default
    private Boolean canEdit = false;
    
    @Column(name = "can_comment")
    @Builder.Default
    private Boolean canComment = false;
    
    @Column(name = "can_share")
    @Builder.Default
    private Boolean canShare = false;
    
    @Column(name = "can_export")
    @Builder.Default
    private Boolean canExport = true;
    
    @Column(name = "can_delete")
    @Builder.Default
    private Boolean canDelete = false;
    
    @Column(name = "can_manage_permissions")
    @Builder.Default
    private Boolean canManagePermissions = false;
    
    // Invitation Status
    @Column(name = "invitation_status", length = 20)
    @Builder.Default
    private String invitationStatus = "accepted";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;
    
    @Column(name = "invited_at")
    private Instant invitedAt;
    
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    
    // Expiration
    @Column(name = "expires_at")
    private Instant expiresAt;
}
```

---

### 7. ActiveCollaborator Entity

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "active_collaborators", indexes = {
    @Index(name = "idx_collaborators_diagram", columnList = "diagram_id"),
    @Index(name = "idx_collaborators_session", columnList = "session_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_session", columnNames = {"diagram_id", "user_id", "session_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveCollaborator extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Session Info
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    @Column(name = "websocket_session_id", length = 100)
    private String websocketSessionId;
    
    // Cursor Position
    @Column(name = "cursor_x", precision = 10, scale = 2)
    private BigDecimal cursorX;
    
    @Column(name = "cursor_y", precision = 10, scale = 2)
    private BigDecimal cursorY;
    
    @Column(name = "cursor_color", length = 7)
    private String cursorColor;
    
    // Denormalized User Info (for performance)
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "user_avatar", length = 500)
    private String userAvatar;
    
    // Selection
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_table_id")
    private DiagramTable selectedTable;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_column_id")
    private TableColumn selectedColumn;
    
    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_idle")
    @Builder.Default
    private Boolean isIdle = false;
    
    @Column(length = 20)
    @Builder.Default
    private String status = "active";
    
    // Device Info
    @Column(name = "device_type", length = 20)
    private String deviceType;
    
    @Column(length = 50)
    private String browser;
    
    // Timestamps
    @Column(name = "connected_at")
    @Builder.Default
    private Instant connectedAt = Instant.now();
    
    @Column(name = "last_seen")
    @Builder.Default
    private Instant lastSeen = Instant.now();
    
    @Column(name = "last_activity")
    @Builder.Default
    private Instant lastActivity = Instant.now();
    
    // Update cursor position
    public void updateCursor(BigDecimal x, BigDecimal y) {
        this.cursorX = x;
        this.cursorY = y;
        this.lastSeen = Instant.now();
        this.lastActivity = Instant.now();
    }
}
```

---

### 8. Enum Types

```java
// PermissionLevel.java
package com.chartdb.model.enums;

public enum PermissionLevel {
    OWNER,
    EDITOR,
    COMMENTER,
    VIEWER
}

// RelationshipType.java
package com.chartdb.model.enums;

public enum RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}

// DiagramStatus.java
package com.chartdb.model.enums;

public enum DiagramStatus {
    ACTIVE,
    ARCHIVED,
    DELETED
}

// ActionType.java
package com.chartdb.model.enums;

public enum ActionType {
    CREATE,
    UPDATE,
    DELETE,
    VIEW,
    EXPORT,
    SHARE,
    LOGIN,
    LOGOUT
}
```

---

### 9. AuditLog Entity

```java
package com.chartdb.model;

import com.chartdb.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_diagram", columnList = "diagram_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_time", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id")
    private Diagram diagram;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    // Action
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "entity_id", length = 36)
    private String entityId;
    
    @Column(name = "entity_name", length = 255)
    private String entityName;
    
    // Changes
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> changes;
    
    // Request Info
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    // Timestamp
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
```

---

### 10. TableLock Entity

```java
package com.chartdb.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "table_locks", indexes = {
    @Index(name = "idx_locks_table", columnList = "table_id"),
    @Index(name = "idx_locks_expiry", columnList = "expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_table_lock", columnNames = {"table_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableLock {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiagramTable table;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "lock_type", length = 20)
    @Builder.Default
    private String lockType = "edit";
    
    @Column(name = "acquired_at")
    @Builder.Default
    private Instant acquiredAt = Instant.now();
    
    @Column(name = "expires_at")
    @Builder.Default
    private Instant expiresAt = Instant.now().plusSeconds(30);
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public void extendLock(int seconds) {
        this.expiresAt = Instant.now().plusSeconds(seconds);
    }
}
```

---

**← Previous:** `04-BACKEND-STRUCTURE.md` | **Next:** `06-BACKEND-REPOSITORIES.md` →
