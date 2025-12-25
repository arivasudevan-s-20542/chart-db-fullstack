# 08 - Backend REST Controllers

## 🌐 REST API Controllers

### 1. AuthController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.LoginRequest;
import com.chartdb.dto.request.RegisterRequest;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Client-side logout (just invalidate token on client)
        // For server-side, you'd add token to blacklist
        return ResponseEntity.noContent().build();
    }
}
```

---

### 2. DiagramController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.request.UpdateDiagramRequest;
import com.chartdb.dto.response.DiagramFullResponse;
import com.chartdb.dto.response.DiagramResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.DiagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/diagrams")
@RequiredArgsConstructor
public class DiagramController {
    
    private final DiagramService diagramService;
    
    /**
     * Create a new diagram
     */
    @PostMapping
    public ResponseEntity<DiagramResponse> createDiagram(
            @Valid @RequestBody CreateDiagramRequest request,
            @CurrentUser UserPrincipal currentUser) {
        DiagramResponse response = diagramService.createDiagram(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get diagram metadata
     */
    @GetMapping("/{diagramId}")
    public ResponseEntity<DiagramResponse> getDiagram(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        DiagramResponse response = diagramService.getDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get full diagram with all tables, columns, and relationships
     * This is the main endpoint for loading a diagram in the editor
     */
    @GetMapping("/{diagramId}/full")
    public ResponseEntity<DiagramFullResponse> getFullDiagram(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        DiagramFullResponse response = diagramService.getFullDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update diagram metadata
     */
    @PutMapping("/{diagramId}")
    public ResponseEntity<DiagramResponse> updateDiagram(
            @PathVariable String diagramId,
            @Valid @RequestBody UpdateDiagramRequest request,
            @CurrentUser UserPrincipal currentUser) {
        DiagramResponse response = diagramService.updateDiagram(diagramId, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update canvas state (zoom, pan)
     */
    @PatchMapping("/{diagramId}/canvas")
    public ResponseEntity<Void> updateCanvasState(
            @PathVariable String diagramId,
            @RequestParam(required = false) BigDecimal zoom,
            @RequestParam(required = false) BigDecimal offsetX,
            @RequestParam(required = false) BigDecimal offsetY,
            @CurrentUser UserPrincipal currentUser) {
        diagramService.updateCanvasState(diagramId, zoom, offsetX, offsetY, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete diagram (soft delete)
     */
    @DeleteMapping("/{diagramId}")
    public ResponseEntity<Void> deleteDiagram(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        diagramService.deleteDiagram(diagramId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get user's diagrams
     */
    @GetMapping("/my")
    public ResponseEntity<Page<DiagramResponse>> getMyDiagrams(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        Page<DiagramResponse> diagrams = diagramService.getUserDiagrams(currentUser.getId(), pageable);
        return ResponseEntity.ok(diagrams);
    }
    
    /**
     * Get public diagrams (for explore/discover)
     */
    @GetMapping("/public")
    public ResponseEntity<Page<DiagramResponse>> getPublicDiagrams(
            @PageableDefault(size = 20, sort = "viewCount", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        Page<DiagramResponse> diagrams = diagramService.getPublicDiagrams(pageable);
        return ResponseEntity.ok(diagrams);
    }
}
```

---

### 3. TableController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.dto.request.UpdatePositionRequest;
import com.chartdb.dto.request.UpdateTableRequest;
import com.chartdb.dto.response.TableResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {
    
    private final TableService tableService;
    
    /**
     * Create a new table
     */
    @PostMapping
    public ResponseEntity<TableResponse> createTable(
            @Valid @RequestBody CreateTableRequest request,
            @CurrentUser UserPrincipal currentUser) {
        TableResponse response = tableService.createTable(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get table by ID
     */
    @GetMapping("/{tableId}")
    public ResponseEntity<TableResponse> getTable(
            @PathVariable String tableId,
            @CurrentUser UserPrincipal currentUser) {
        TableResponse response = tableService.getTable(tableId, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update table
     */
    @PutMapping("/{tableId}")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable String tableId,
            @Valid @RequestBody UpdateTableRequest request,
            @CurrentUser UserPrincipal currentUser) {
        TableResponse response = tableService.updateTable(tableId, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * CRITICAL: Update table position
     * This endpoint is called when user finishes dragging a table
     */
    @PutMapping("/{tableId}/position")
    public ResponseEntity<Void> updateTablePosition(
            @PathVariable String tableId,
            @RequestParam BigDecimal x,
            @RequestParam BigDecimal y,
            @CurrentUser UserPrincipal currentUser) {
        tableService.updatePosition(tableId, x, y, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Alternative: Update position via request body
     */
    @PatchMapping("/{tableId}/position")
    public ResponseEntity<Void> updateTablePositionBody(
            @PathVariable String tableId,
            @Valid @RequestBody UpdatePositionRequest request,
            @CurrentUser UserPrincipal currentUser) {
        tableService.updatePosition(tableId, request.getX(), request.getY(), currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Batch update positions (for multi-select drag)
     */
    @PostMapping("/batch/position")
    public ResponseEntity<Void> updatePositionsBatch(
            @RequestBody BatchPositionUpdateRequest request,
            @CurrentUser UserPrincipal currentUser) {
        tableService.updatePositionsBatch(
            request.getTableIds(), 
            request.getDeltaX(), 
            request.getDeltaY(), 
            currentUser.getId()
        );
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete table
     */
    @DeleteMapping("/{tableId}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable String tableId,
            @CurrentUser UserPrincipal currentUser) {
        tableService.deleteTable(tableId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get all tables for a diagram
     */
    @GetMapping("/diagram/{diagramId}")
    public ResponseEntity<List<TableResponse>> getTablesByDiagram(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        List<TableResponse> tables = tableService.getTablesByDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(tables);
    }
    
    // Inner class for batch request
    @lombok.Data
    public static class BatchPositionUpdateRequest {
        private List<String> tableIds;
        private BigDecimal deltaX;
        private BigDecimal deltaY;
    }
}
```

---

### 4. ColumnController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.request.UpdateColumnRequest;
import com.chartdb.dto.response.ColumnResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.ColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/columns")
@RequiredArgsConstructor
public class ColumnController {
    
    private final ColumnService columnService;
    
    @PostMapping
    public ResponseEntity<ColumnResponse> createColumn(
            @Valid @RequestBody CreateColumnRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ColumnResponse response = columnService.createColumn(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{columnId}")
    public ResponseEntity<ColumnResponse> updateColumn(
            @PathVariable String columnId,
            @Valid @RequestBody UpdateColumnRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ColumnResponse response = columnService.updateColumn(columnId, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable String columnId,
            @CurrentUser UserPrincipal currentUser) {
        columnService.deleteColumn(columnId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<ColumnResponse>> getColumnsByTable(
            @PathVariable String tableId,
            @CurrentUser UserPrincipal currentUser) {
        List<ColumnResponse> columns = columnService.getColumnsByTable(tableId, currentUser.getId());
        return ResponseEntity.ok(columns);
    }
    
    @PostMapping("/table/{tableId}/reorder")
    public ResponseEntity<Void> reorderColumns(
            @PathVariable String tableId,
            @RequestBody List<String> columnIds,
            @CurrentUser UserPrincipal currentUser) {
        columnService.reorderColumns(tableId, columnIds, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
```

---

### 5. RelationshipController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.CreateRelationshipRequest;
import com.chartdb.dto.request.UpdateRelationshipRequest;
import com.chartdb.dto.response.RelationshipResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.RelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relationships")
@RequiredArgsConstructor
public class RelationshipController {
    
    private final RelationshipService relationshipService;
    
    @PostMapping
    public ResponseEntity<RelationshipResponse> createRelationship(
            @Valid @RequestBody CreateRelationshipRequest request,
            @CurrentUser UserPrincipal currentUser) {
        RelationshipResponse response = relationshipService.createRelationship(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{relationshipId}")
    public ResponseEntity<RelationshipResponse> updateRelationship(
            @PathVariable String relationshipId,
            @Valid @RequestBody UpdateRelationshipRequest request,
            @CurrentUser UserPrincipal currentUser) {
        RelationshipResponse response = relationshipService.updateRelationship(
            relationshipId, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deleteRelationship(
            @PathVariable String relationshipId,
            @CurrentUser UserPrincipal currentUser) {
        relationshipService.deleteRelationship(relationshipId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/diagram/{diagramId}")
    public ResponseEntity<List<RelationshipResponse>> getRelationshipsByDiagram(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        List<RelationshipResponse> relationships = relationshipService
            .getRelationshipsByDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(relationships);
    }
    
    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<RelationshipResponse>> getRelationshipsByTable(
            @PathVariable String tableId,
            @CurrentUser UserPrincipal currentUser) {
        List<RelationshipResponse> relationships = relationshipService
            .getRelationshipsByTable(tableId, currentUser.getId());
        return ResponseEntity.ok(relationships);
    }
}
```

---

### 6. CollaboratorController

```java
package com.chartdb.controller;

import com.chartdb.dto.response.CollaboratorResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collaborators")
@RequiredArgsConstructor
public class CollaboratorController {
    
    private final CollaborationService collaborationService;
    
    /**
     * Get active collaborators in a diagram
     */
    @GetMapping("/diagram/{diagramId}")
    public ResponseEntity<List<CollaboratorResponse>> getActiveCollaborators(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        List<CollaboratorResponse> collaborators = collaborationService.getActiveCollaborators(diagramId);
        return ResponseEntity.ok(collaborators);
    }
    
    /**
     * Get collaborator count for a diagram
     */
    @GetMapping("/diagram/{diagramId}/count")
    public ResponseEntity<Long> getCollaboratorCount(@PathVariable String diagramId) {
        long count = collaborationService.getActiveCollaboratorCount(diagramId);
        return ResponseEntity.ok(count);
    }
}
```

---

### 7. UserController

```java
package com.chartdb.controller;

import com.chartdb.dto.request.UpdateUserRequest;
import com.chartdb.dto.response.UserResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.getUser(currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update current user's profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search users (for sharing)
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String query,
            @CurrentUser UserPrincipal currentUser) {
        List<UserResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID (for collaborator info)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable String userId,
            @CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }
}
```

---

### 8. ExportController

```java
package com.chartdb.controller;

import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {
    
    private final ExportService exportService;
    
    /**
     * Export diagram as SQL DDL
     */
    @GetMapping("/diagram/{diagramId}/sql")
    public ResponseEntity<String> exportAsSql(
            @PathVariable String diagramId,
            @RequestParam(defaultValue = "postgresql") String dialect,
            @CurrentUser UserPrincipal currentUser) {
        String sql = exportService.exportAsSql(diagramId, dialect, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"schema.sql\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(sql);
    }
    
    /**
     * Export diagram as JSON
     */
    @GetMapping("/diagram/{diagramId}/json")
    public ResponseEntity<String> exportAsJson(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        String json = exportService.exportAsJson(diagramId, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diagram.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json);
    }
    
    /**
     * Export diagram as PNG (server-side rendering - optional)
     */
    @GetMapping("/diagram/{diagramId}/png")
    public ResponseEntity<byte[]> exportAsPng(
            @PathVariable String diagramId,
            @RequestParam(defaultValue = "2") int scale,
            @CurrentUser UserPrincipal currentUser) {
        byte[] image = exportService.exportAsPng(diagramId, scale, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diagram.png\"")
            .contentType(MediaType.IMAGE_PNG)
            .body(image);
    }
}
```

---

## 📝 Request/Response DTOs

### Request DTOs

```java
// CreateDiagramRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDiagramRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;
    
    private String description;
    private String databaseType;
}

// CreateTableRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateTableRequest {
    @NotNull(message = "Diagram ID is required")
    private String diagramId;
    
    @NotBlank(message = "Table name is required")
    private String name;
    
    private String displayName;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private String color;
    private List<CreateColumnRequest> columns;
}

// CreateColumnRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateColumnRequest {
    @NotNull(message = "Table ID is required")
    private String tableId;
    
    @NotBlank(message = "Column name is required")
    private String name;
    
    @NotBlank(message = "Data type is required")
    private String dataType;
    
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isNullable;
    private Boolean isUnique;
    private String defaultValue;
}

// UpdatePositionRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdatePositionRequest {
    @NotNull(message = "X position is required")
    private BigDecimal x;
    
    @NotNull(message = "Y position is required")
    private BigDecimal y;
}

// LoginRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}

// RegisterRequest.java
package com.chartdb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    private String displayName;
}
```

### Response DTOs

```java
// AuthResponse.java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String cursorColor;
    }
}

// DiagramFullResponse.java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class DiagramFullResponse {
    private String id;
    private String name;
    private String description;
    private String databaseType;
    private BigDecimal canvasZoom;
    private BigDecimal canvasOffsetX;
    private BigDecimal canvasOffsetY;
    private Boolean isPublic;
    private String ownerId;
    private String ownerName;
    private List<TableDTO> tables;
    private List<RelationshipDTO> relationships;
    private Instant createdAt;
    private Instant updatedAt;
    
    @Data
    @Builder
    public static class TableDTO {
        private String id;
        private String name;
        private String displayName;
        private BigDecimal positionX;  // CRITICAL
        private BigDecimal positionY;  // CRITICAL
        private BigDecimal width;
        private BigDecimal height;
        private String color;
        private Boolean isCollapsed;
        private List<ColumnDTO> columns;
    }
    
    @Data
    @Builder
    public static class ColumnDTO {
        private String id;
        private String name;
        private String dataType;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private Boolean isNullable;
        private Boolean isUnique;
        private String defaultValue;
        private Integer orderIndex;
    }
    
    @Data
    @Builder
    public static class RelationshipDTO {
        private String id;
        private String sourceTableId;
        private String targetTableId;
        private String sourceColumnId;
        private String targetColumnId;
        private String relationshipType;
        private String name;
    }
}

// CollaboratorResponse.java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class CollaboratorResponse {
    private String userId;
    private String userName;
    private String avatarUrl;
    private String cursorColor;
    private Double cursorX;
    private Double cursorY;
    private Boolean isIdle;
    private Instant connectedAt;
}

// ErrorResponse.java
package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;
    
    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
```

---

## ⚠️ Global Exception Handler

```java
package com.chartdb.exception;

import com.chartdb.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(error -> ErrorResponse.FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build())
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Validation failed")
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred", 
            request
        );
    }
    
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
```

---

**← Previous:** `07-BACKEND-SERVICES.md` | **Next:** `09-WEBSOCKET-CONFIG.md` →
