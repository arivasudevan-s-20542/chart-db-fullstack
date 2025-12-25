# 11 - DTOs and Mappers

## 📦 Data Transfer Objects

### User DTOs

```java
// UserDTO.java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Boolean emailVerified;
    private String provider;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

// UserRegistrationRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;
    
    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be 2-100 characters")
    private String displayName;
}

// UserLoginRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}

// AuthResponse.java
package com.chartdb.dto.response;

import com.chartdb.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDTO user;
}
```

---

### Diagram DTOs

```java
// DiagramDTO.java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DiagramDTO {
    private UUID id;
    private String name;
    private String description;
    private String databaseType;
    private Boolean isPublic;
    private UUID ownerId;
    private String ownerName;
    
    // Diagram metadata
    private Double zoomLevel;
    private Double panX;
    private Double panY;
    private String theme;
    
    // Nested data (optional - based on includeRelations parameter)
    private List<TableDTO> tables;
    private List<RelationshipDTO> relationships;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// DiagramSummaryDTO.java (for list views)
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DiagramSummaryDTO {
    private UUID id;
    private String name;
    private String description;
    private String databaseType;
    private Boolean isPublic;
    private Integer tableCount;
    private Integer relationshipCount;
    private String ownerName;
    private String thumbnailUrl;
    private LocalDateTime updatedAt;
}

// CreateDiagramRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDiagramRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be 1-255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;
    
    @NotBlank(message = "Database type is required")
    private String databaseType;  // postgresql, mysql, sqlite, etc.
    
    private Boolean isPublic = false;
    
    // Optional: import from SQL
    private String importSql;
}

// UpdateDiagramRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDiagramRequest {
    
    @Size(min = 1, max = 255, message = "Name must be 1-255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;
    
    private Boolean isPublic;
    
    // Canvas state
    private Double zoomLevel;
    private Double panX;
    private Double panY;
    private String theme;
}

// DiagramPositionUpdate.java (for canvas pan/zoom)
package com.chartdb.dto.request;

import lombok.Data;

@Data
public class DiagramPositionUpdate {
    private Double zoomLevel;
    private Double panX;
    private Double panY;
}
```

---

### Table DTOs

```java
// TableDTO.java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TableDTO {
    private UUID id;
    private UUID diagramId;
    private String name;
    private String schema;
    private String comment;
    private String color;
    private Double positionX;
    private Double positionY;
    private Integer width;
    private Boolean isCollapsed;
    private Integer displayOrder;
    
    // Nested columns (optional)
    private List<ColumnDTO> columns;
    
    // Computed fields
    private Integer columnCount;
    private Boolean hasPrimaryKey;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// CreateTableRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTableRequest {
    
    @NotBlank(message = "Table name is required")
    @Size(min = 1, max = 255, message = "Table name must be 1-255 characters")
    private String name;
    
    private String schema = "public";
    
    private String comment;
    
    private String color = "#3B82F6";  // Default blue
    
    @NotNull(message = "Position X is required")
    private Double positionX;
    
    @NotNull(message = "Position Y is required")
    private Double positionY;
    
    private Integer width = 200;
}

// UpdateTableRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTableRequest {
    
    @Size(min = 1, max = 255, message = "Table name must be 1-255 characters")
    private String name;
    
    private String schema;
    private String comment;
    private String color;
    private Double positionX;
    private Double positionY;
    private Integer width;
    private Boolean isCollapsed;
}

// TablePositionUpdate.java (for drag operations)
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TablePositionUpdate {
    
    @NotNull(message = "Position X is required")
    private Double positionX;
    
    @NotNull(message = "Position Y is required")
    private Double positionY;
}

// BulkTablePositionUpdate.java (for moving multiple tables)
package com.chartdb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkTablePositionUpdate {
    
    @NotEmpty(message = "At least one position update required")
    @Valid
    private List<TablePosition> positions;
    
    @Data
    public static class TablePosition {
        private UUID tableId;
        private Double positionX;
        private Double positionY;
    }
}
```

---

### Column DTOs

```java
// ColumnDTO.java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ColumnDTO {
    private UUID id;
    private UUID tableId;
    private String name;
    private String dataType;
    private String rawType;
    private Boolean isPrimaryKey;
    private Boolean isNullable;
    private Boolean isUnique;
    private Boolean isAutoIncrement;
    private String defaultValue;
    private String comment;
    private Integer displayOrder;
    
    // Character/Numeric precision (optional)
    private Integer characterMaxLength;
    private Integer numericPrecision;
    private Integer numericScale;
    
    // Computed
    private String displayType;  // e.g., "VARCHAR(255)"
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// CreateColumnRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateColumnRequest {
    
    @NotBlank(message = "Column name is required")
    @Size(min = 1, max = 255, message = "Column name must be 1-255 characters")
    private String name;
    
    @NotBlank(message = "Data type is required")
    private String dataType;
    
    private String rawType;
    private Boolean isPrimaryKey = false;
    private Boolean isNullable = true;
    private Boolean isUnique = false;
    private Boolean isAutoIncrement = false;
    private String defaultValue;
    private String comment;
    private Integer characterMaxLength;
    private Integer numericPrecision;
    private Integer numericScale;
}

// UpdateColumnRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateColumnRequest {
    
    @Size(min = 1, max = 255, message = "Column name must be 1-255 characters")
    private String name;
    
    private String dataType;
    private String rawType;
    private Boolean isPrimaryKey;
    private Boolean isNullable;
    private Boolean isUnique;
    private Boolean isAutoIncrement;
    private String defaultValue;
    private String comment;
    private Integer characterMaxLength;
    private Integer numericPrecision;
    private Integer numericScale;
}

// ReorderColumnsRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderColumnsRequest {
    
    @NotEmpty(message = "Column order list is required")
    private List<UUID> columnIds;  // Ordered list of column IDs
}
```

---

### Relationship DTOs

```java
// RelationshipDTO.java
package com.chartdb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RelationshipDTO {
    private UUID id;
    private UUID diagramId;
    
    // Source (FK side)
    private UUID sourceTableId;
    private String sourceTableName;
    private UUID sourceColumnId;
    private String sourceColumnName;
    
    // Target (PK side)
    private UUID targetTableId;
    private String targetTableName;
    private UUID targetColumnId;
    private String targetColumnName;
    
    // Relationship type
    private String cardinality;  // one_to_one, one_to_many, many_to_many
    private String name;  // Constraint name
    
    // Visual properties
    private String style;  // straight, curved, orthogonal
    private String color;
    
    private LocalDateTime createdAt;
}

// CreateRelationshipRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRelationshipRequest {
    
    @NotNull(message = "Source table ID is required")
    private UUID sourceTableId;
    
    @NotNull(message = "Source column ID is required")
    private UUID sourceColumnId;
    
    @NotNull(message = "Target table ID is required")
    private UUID targetTableId;
    
    @NotNull(message = "Target column ID is required")
    private UUID targetColumnId;
    
    private String cardinality = "one_to_many";
    
    private String name;  // Optional: auto-generated if not provided
    
    private String style = "curved";
    private String color;
}

// UpdateRelationshipRequest.java
package com.chartdb.dto.request;

import lombok.Data;

@Data
public class UpdateRelationshipRequest {
    private String cardinality;
    private String name;
    private String style;
    private String color;
}
```

---

## 🔄 Mapper Classes (MapStruct)

### User Mapper

```java
package com.chartdb.mapper;

import com.chartdb.dto.UserDTO;
import com.chartdb.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserDTO toDTO(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserDTO dto);
}
```

### Diagram Mapper

```java
package com.chartdb.mapper;

import com.chartdb.dto.DiagramDTO;
import com.chartdb.dto.DiagramSummaryDTO;
import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.entity.Diagram;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {TableMapper.class, RelationshipMapper.class})
public interface DiagramMapper {
    
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.displayName")
    DiagramDTO toDTO(Diagram diagram);
    
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.displayName")
    @Mapping(target = "tableCount", expression = "java(diagram.getTables() != null ? diagram.getTables().size() : 0)")
    @Mapping(target = "relationshipCount", expression = "java(diagram.getRelationships() != null ? diagram.getRelationships().size() : 0)")
    DiagramSummaryDTO toSummaryDTO(Diagram diagram);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "tables", ignore = true)
    @Mapping(target = "relationships", ignore = true)
    @Mapping(target = "collaborators", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Diagram toEntity(CreateDiagramRequest request);
    
    List<DiagramDTO> toDTOList(List<Diagram> diagrams);
    List<DiagramSummaryDTO> toSummaryDTOList(List<Diagram> diagrams);
}
```

### Table Mapper

```java
package com.chartdb.mapper;

import com.chartdb.dto.TableDTO;
import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.entity.Table;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ColumnMapper.class})
public interface TableMapper {
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "columnCount", expression = "java(table.getColumns() != null ? table.getColumns().size() : 0)")
    @Mapping(target = "hasPrimaryKey", expression = "java(hasPrimaryKey(table))")
    TableDTO toDTO(Table table);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "columns", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Table toEntity(CreateTableRequest request);
    
    List<TableDTO> toDTOList(List<Table> tables);
    
    default boolean hasPrimaryKey(Table table) {
        if (table.getColumns() == null) return false;
        return table.getColumns().stream()
            .anyMatch(col -> Boolean.TRUE.equals(col.getIsPrimaryKey()));
    }
}
```

### Column Mapper

```java
package com.chartdb.mapper;

import com.chartdb.dto.ColumnDTO;
import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.entity.Column;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ColumnMapper {
    
    @Mapping(target = "tableId", source = "table.id")
    @Mapping(target = "displayType", expression = "java(buildDisplayType(column))")
    ColumnDTO toDTO(Column column);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Column toEntity(CreateColumnRequest request);
    
    List<ColumnDTO> toDTOList(List<Column> columns);
    
    default String buildDisplayType(Column column) {
        StringBuilder sb = new StringBuilder(column.getDataType().toUpperCase());
        
        if (column.getCharacterMaxLength() != null) {
            sb.append("(").append(column.getCharacterMaxLength()).append(")");
        } else if (column.getNumericPrecision() != null) {
            sb.append("(").append(column.getNumericPrecision());
            if (column.getNumericScale() != null && column.getNumericScale() > 0) {
                sb.append(",").append(column.getNumericScale());
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
}
```

### Relationship Mapper

```java
package com.chartdb.mapper;

import com.chartdb.dto.RelationshipDTO;
import com.chartdb.dto.request.CreateRelationshipRequest;
import com.chartdb.entity.Relationship;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RelationshipMapper {
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "sourceTableId", source = "sourceTable.id")
    @Mapping(target = "sourceTableName", source = "sourceTable.name")
    @Mapping(target = "sourceColumnId", source = "sourceColumn.id")
    @Mapping(target = "sourceColumnName", source = "sourceColumn.name")
    @Mapping(target = "targetTableId", source = "targetTable.id")
    @Mapping(target = "targetTableName", source = "targetTable.name")
    @Mapping(target = "targetColumnId", source = "targetColumn.id")
    @Mapping(target = "targetColumnName", source = "targetColumn.name")
    RelationshipDTO toDTO(Relationship relationship);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "sourceTable", ignore = true)
    @Mapping(target = "sourceColumn", ignore = true)
    @Mapping(target = "targetTable", ignore = true)
    @Mapping(target = "targetColumn", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Relationship toEntity(CreateRelationshipRequest request);
    
    List<RelationshipDTO> toDTOList(List<Relationship> relationships);
}
```

---

## 📄 Pagination Response

```java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
}

// Usage in service:
public PageResponse<DiagramSummaryDTO> toDiagramPageResponse(Page<Diagram> page) {
    return PageResponse.<DiagramSummaryDTO>builder()
        .content(diagramMapper.toSummaryDTOList(page.getContent()))
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .hasNext(page.hasNext())
        .hasPrevious(page.hasPrevious())
        .build();
}
```

---

## 🚨 Error Response DTOs

```java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, List<String>> fieldErrors;  // For validation errors
}

// Usage in GlobalExceptionHandler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));
        
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .status(400)
                .error("Validation Error")
                .message("Invalid request data")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build()
        );
    }
}
```

---

**← Previous:** `10-WEBSOCKET-HANDLERS.md` | **Next:** `12-FRONTEND-INTEGRATION.md` →
