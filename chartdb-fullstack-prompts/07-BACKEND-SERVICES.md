# 07 - Backend Services

## 🔧 Service Layer Implementation

### 1. AuthService

```java
package com.chartdb.service;

import com.chartdb.dto.request.LoginRequest;
import com.chartdb.dto.request.RegisterRequest;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.UnauthorizedException;
import com.chartdb.model.User;
import com.chartdb.repository.UserRepository;
import com.chartdb.security.JwtTokenProvider;
import com.chartdb.util.ColorGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BadRequestException("Email already registered");
        }
        
        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        
        // Create user
        User user = User.builder()
            .id(UUID.randomUUID().toString())
            .email(request.getEmail().toLowerCase())
            .username(request.getUsername())
            .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .cursorColor(ColorGenerator.generateRandomColor())
            .isActive(true)
            .isVerified(false)
            .build();
        
        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
        
        // Generate tokens
        return generateAuthResponse(user);
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }
        
        // Update last login
        userRepository.updateLastLogin(user.getId(), Instant.now());
        log.info("User logged in: {}", user.getEmail());
        
        return generateAuthResponse(user);
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }
        
        return generateAuthResponse(user);
    }
    
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .cursorColor(user.getCursorColor())
                .build())
            .build();
    }
}
```

---

### 2. DiagramService

```java
package com.chartdb.service;

import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.request.UpdateDiagramRequest;
import com.chartdb.dto.response.DiagramFullResponse;
import com.chartdb.dto.response.DiagramResponse;
import com.chartdb.exception.ForbiddenException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.*;
import com.chartdb.model.enums.DiagramStatus;
import com.chartdb.model.enums.PermissionLevel;
import com.chartdb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramService {
    
    private final DiagramRepository diagramRepository;
    private final TableRepository tableRepository;
    private final RelationshipRepository relationshipRepository;
    private final DiagramPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    
    @Transactional
    public DiagramResponse createDiagram(CreateDiagramRequest request, String userId) {
        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Diagram diagram = Diagram.builder()
            .id(UUID.randomUUID().toString())
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .databaseType(request.getDatabaseType() != null ? request.getDatabaseType() : "postgresql")
            .canvasZoom(BigDecimal.ONE)
            .canvasOffsetX(BigDecimal.ZERO)
            .canvasOffsetY(BigDecimal.ZERO)
            .status(DiagramStatus.ACTIVE)
            .build();
        
        diagram = diagramRepository.save(diagram);
        
        // Create owner permission
        DiagramPermission ownerPermission = DiagramPermission.builder()
            .id(UUID.randomUUID().toString())
            .diagram(diagram)
            .user(owner)
            .permissionLevel(PermissionLevel.OWNER)
            .canView(true)
            .canEdit(true)
            .canComment(true)
            .canShare(true)
            .canExport(true)
            .canDelete(true)
            .canManagePermissions(true)
            .build();
        permissionRepository.save(ownerPermission);
        
        log.info("Diagram created: {} by user {}", diagram.getId(), userId);
        return mapToResponse(diagram);
    }
    
    @Transactional(readOnly = true)
    public DiagramResponse getDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramOrThrow(diagramId);
        
        // Check access
        if (!permissionService.canView(diagramId, userId) && !diagram.getIsPublic()) {
            throw new ForbiddenException("Access denied");
        }
        
        // Update last accessed
        diagramRepository.updateLastAccessed(diagramId, Instant.now());
        diagramRepository.incrementViewCount(diagramId);
        
        return mapToResponse(diagram);
    }
    
    @Transactional(readOnly = true)
    public DiagramFullResponse getFullDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramOrThrow(diagramId);
        
        // Check access
        if (!permissionService.canView(diagramId, userId) && !diagram.getIsPublic()) {
            throw new ForbiddenException("Access denied");
        }
        
        // Get all tables with columns
        List<DiagramTable> tables = tableRepository.findByDiagramIdWithColumns(diagramId);
        
        // Get all relationships
        List<Relationship> relationships = relationshipRepository.findByDiagramIdWithTables(diagramId);
        
        // Update last accessed
        diagramRepository.updateLastAccessed(diagramId, Instant.now());
        
        return DiagramFullResponse.builder()
            .id(diagram.getId())
            .name(diagram.getName())
            .description(diagram.getDescription())
            .databaseType(diagram.getDatabaseType())
            .canvasZoom(diagram.getCanvasZoom())
            .canvasOffsetX(diagram.getCanvasOffsetX())
            .canvasOffsetY(diagram.getCanvasOffsetY())
            .isPublic(diagram.getIsPublic())
            .ownerId(diagram.getOwner().getId())
            .ownerName(diagram.getOwner().getEffectiveDisplayName())
            .tables(tables.stream()
                .map(this::mapTableToResponse)
                .collect(Collectors.toList()))
            .relationships(relationships.stream()
                .map(this::mapRelationshipToResponse)
                .collect(Collectors.toList()))
            .createdAt(diagram.getCreatedAt())
            .updatedAt(diagram.getUpdatedAt())
            .build();
    }
    
    @Transactional
    public DiagramResponse updateDiagram(String diagramId, UpdateDiagramRequest request, String userId) {
        Diagram diagram = findDiagramOrThrow(diagramId);
        
        // Check edit permission
        if (!permissionService.canEdit(diagramId, userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        // Update fields
        if (request.getName() != null) {
            diagram.setName(request.getName());
        }
        if (request.getDescription() != null) {
            diagram.setDescription(request.getDescription());
        }
        if (request.getDatabaseType() != null) {
            diagram.setDatabaseType(request.getDatabaseType());
        }
        if (request.getIsPublic() != null) {
            diagram.setIsPublic(request.getIsPublic());
        }
        
        diagram = diagramRepository.save(diagram);
        log.info("Diagram updated: {} by user {}", diagramId, userId);
        
        return mapToResponse(diagram);
    }
    
    @Transactional
    public void updateCanvasState(String diagramId, BigDecimal zoom, BigDecimal offsetX, 
                                   BigDecimal offsetY, String userId) {
        if (!permissionService.canEdit(diagramId, userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        diagramRepository.updateCanvasState(diagramId, zoom, offsetX, offsetY, Instant.now());
    }
    
    @Transactional
    public void deleteDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramOrThrow(diagramId);
        
        // Only owner can delete
        if (!diagram.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the owner can delete this diagram");
        }
        
        diagram.setStatus(DiagramStatus.DELETED);
        diagramRepository.save(diagram);
        log.info("Diagram deleted: {} by user {}", diagramId, userId);
    }
    
    @Transactional(readOnly = true)
    public Page<DiagramResponse> getUserDiagrams(String userId, Pageable pageable) {
        return diagramRepository.findAccessibleByUser(userId, DiagramStatus.ACTIVE, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<DiagramResponse> getPublicDiagrams(Pageable pageable) {
        return diagramRepository.findByIsPublicTrueAndStatus(DiagramStatus.ACTIVE, pageable)
            .map(this::mapToResponse);
    }
    
    // Helper methods
    private Diagram findDiagramOrThrow(String diagramId) {
        return diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram not found: " + diagramId));
    }
    
    private DiagramResponse mapToResponse(Diagram diagram) {
        return DiagramResponse.builder()
            .id(diagram.getId())
            .name(diagram.getName())
            .description(diagram.getDescription())
            .databaseType(diagram.getDatabaseType())
            .canvasZoom(diagram.getCanvasZoom())
            .canvasOffsetX(diagram.getCanvasOffsetX())
            .canvasOffsetY(diagram.getCanvasOffsetY())
            .isPublic(diagram.getIsPublic())
            .ownerId(diagram.getOwner().getId())
            .ownerName(diagram.getOwner().getEffectiveDisplayName())
            .tableCount((int) tableRepository.countByDiagramId(diagram.getId()))
            .createdAt(diagram.getCreatedAt())
            .updatedAt(diagram.getUpdatedAt())
            .build();
    }
    
    private DiagramFullResponse.TableDTO mapTableToResponse(DiagramTable table) {
        return DiagramFullResponse.TableDTO.builder()
            .id(table.getId())
            .name(table.getName())
            .displayName(table.getDisplayName())
            .positionX(table.getPositionX())
            .positionY(table.getPositionY())
            .width(table.getWidth())
            .height(table.getHeight())
            .color(table.getColor())
            .isCollapsed(table.getIsCollapsed())
            .columns(table.getColumns().stream()
                .map(this::mapColumnToResponse)
                .collect(Collectors.toList()))
            .build();
    }
    
    private DiagramFullResponse.ColumnDTO mapColumnToResponse(TableColumn column) {
        return DiagramFullResponse.ColumnDTO.builder()
            .id(column.getId())
            .name(column.getName())
            .dataType(column.getDataType())
            .isPrimaryKey(column.getIsPrimaryKey())
            .isForeignKey(column.getIsForeignKey())
            .isNullable(column.getIsNullable())
            .isUnique(column.getIsUnique())
            .defaultValue(column.getDefaultValue())
            .orderIndex(column.getOrderIndex())
            .build();
    }
    
    private DiagramFullResponse.RelationshipDTO mapRelationshipToResponse(Relationship rel) {
        return DiagramFullResponse.RelationshipDTO.builder()
            .id(rel.getId())
            .sourceTableId(rel.getSourceTable().getId())
            .targetTableId(rel.getTargetTable().getId())
            .sourceColumnId(rel.getSourceColumn() != null ? rel.getSourceColumn().getId() : null)
            .targetColumnId(rel.getTargetColumn() != null ? rel.getTargetColumn().getId() : null)
            .relationshipType(rel.getRelationshipType().name())
            .name(rel.getName())
            .build();
    }
}
```

---

### 3. TableService

```java
package com.chartdb.service;

import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.dto.request.UpdateTableRequest;
import com.chartdb.dto.response.TableResponse;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ForbiddenException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramTable;
import com.chartdb.model.TableColumn;
import com.chartdb.repository.ColumnRepository;
import com.chartdb.repository.DiagramRepository;
import com.chartdb.repository.RelationshipRepository;
import com.chartdb.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {
    
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final RelationshipRepository relationshipRepository;
    private final DiagramRepository diagramRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    
    @Transactional
    public TableResponse createTable(CreateTableRequest request, String userId) {
        // Check edit permission
        if (!permissionService.canEdit(request.getDiagramId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        Diagram diagram = diagramRepository.findById(request.getDiagramId())
            .orElseThrow(() -> new ResourceNotFoundException("Diagram not found"));
        
        // Check unique name
        if (tableRepository.existsByDiagramIdAndName(request.getDiagramId(), request.getName())) {
            throw new BadRequestException("Table name already exists in this diagram");
        }
        
        // Get next sort order
        Integer nextSortOrder = tableRepository.getMaxSortOrder(request.getDiagramId()) + 1;
        
        // Create table
        DiagramTable table = DiagramTable.builder()
            .id(UUID.randomUUID().toString())
            .diagram(diagram)
            .name(request.getName())
            .displayName(request.getDisplayName())
            .positionX(request.getPositionX() != null ? request.getPositionX() : BigDecimal.valueOf(100))
            .positionY(request.getPositionY() != null ? request.getPositionY() : BigDecimal.valueOf(100))
            .width(request.getWidth() != null ? request.getWidth() : BigDecimal.valueOf(200))
            .color(request.getColor() != null ? request.getColor() : "#6366F1")
            .sortOrder(nextSortOrder)
            .build();
        
        table = tableRepository.save(table);
        
        // Create columns if provided
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            for (int i = 0; i < request.getColumns().size(); i++) {
                var colRequest = request.getColumns().get(i);
                TableColumn column = TableColumn.builder()
                    .id(UUID.randomUUID().toString())
                    .table(table)
                    .name(colRequest.getName())
                    .dataType(colRequest.getDataType())
                    .isPrimaryKey(colRequest.getIsPrimaryKey() != null ? colRequest.getIsPrimaryKey() : false)
                    .isNullable(colRequest.getIsNullable() != null ? colRequest.getIsNullable() : true)
                    .orderIndex(i)
                    .build();
                columnRepository.save(column);
                table.getColumns().add(column);
            }
        }
        
        log.info("Table created: {} in diagram {} by user {}", table.getId(), request.getDiagramId(), userId);
        
        // Notify collaborators
        notificationService.broadcastTableCreated(request.getDiagramId(), table, userId);
        
        return mapToResponse(table);
    }
    
    @Transactional(readOnly = true)
    public TableResponse getTable(String tableId, String userId) {
        DiagramTable table = tableRepository.findByIdWithColumns(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        
        if (!permissionService.canView(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Access denied");
        }
        
        return mapToResponse(table);
    }
    
    @Transactional
    public TableResponse updateTable(String tableId, UpdateTableRequest request, String userId) {
        DiagramTable table = findTableOrThrow(tableId);
        
        if (!permissionService.canEdit(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        // Check unique name if changing
        if (request.getName() != null && !request.getName().equals(table.getName())) {
            if (tableRepository.existsByDiagramIdAndNameAndIdNot(
                    table.getDiagram().getId(), request.getName(), tableId)) {
                throw new BadRequestException("Table name already exists");
            }
            table.setName(request.getName());
        }
        
        // Update other fields
        if (request.getDisplayName() != null) table.setDisplayName(request.getDisplayName());
        if (request.getColor() != null) table.setColor(request.getColor());
        if (request.getIsCollapsed() != null) table.setIsCollapsed(request.getIsCollapsed());
        
        table = tableRepository.save(table);
        log.info("Table updated: {} by user {}", tableId, userId);
        
        // Notify collaborators
        notificationService.broadcastTableUpdated(table.getDiagram().getId(), table, userId);
        
        return mapToResponse(table);
    }
    
    /**
     * CRITICAL METHOD: Update table position
     * This must persist position_x and position_y correctly
     */
    @Transactional
    public void updatePosition(String tableId, BigDecimal x, BigDecimal y, String userId) {
        DiagramTable table = findTableOrThrow(tableId);
        
        if (!permissionService.canEdit(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        // CRITICAL: Save position
        tableRepository.updatePosition(tableId, x, y, Instant.now());
        
        log.debug("Table position updated: {} to ({}, {})", tableId, x, y);
    }
    
    /**
     * Convenience method for double values
     */
    @Transactional
    public void updatePosition(String tableId, double x, double y, String userId) {
        updatePosition(tableId, BigDecimal.valueOf(x), BigDecimal.valueOf(y), userId);
    }
    
    /**
     * Batch update positions for multi-select drag
     */
    @Transactional
    public void updatePositionsBatch(List<String> tableIds, BigDecimal deltaX, BigDecimal deltaY, String userId) {
        if (tableIds.isEmpty()) return;
        
        // Verify all tables belong to same diagram and user has permission
        DiagramTable firstTable = findTableOrThrow(tableIds.get(0));
        String diagramId = firstTable.getDiagram().getId();
        
        if (!permissionService.canEdit(diagramId, userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        tableRepository.updatePositionsByDelta(tableIds, deltaX, deltaY, Instant.now());
        log.debug("Batch position update for {} tables by delta ({}, {})", tableIds.size(), deltaX, deltaY);
    }
    
    @Transactional
    public void deleteTable(String tableId, String userId) {
        DiagramTable table = findTableOrThrow(tableId);
        
        if (!permissionService.canEdit(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        String diagramId = table.getDiagram().getId();
        
        // Delete relationships connected to this table
        relationshipRepository.deleteByTableId(tableId);
        
        // Clear FK references in other tables
        columnRepository.clearForeignKeyReferences(tableId);
        
        // Delete the table (cascades to columns)
        tableRepository.delete(table);
        
        log.info("Table deleted: {} by user {}", tableId, userId);
        
        // Notify collaborators
        notificationService.broadcastTableDeleted(diagramId, tableId, userId);
    }
    
    @Transactional(readOnly = true)
    public List<TableResponse> getTablesByDiagram(String diagramId, String userId) {
        if (!permissionService.canView(diagramId, userId)) {
            throw new ForbiddenException("Access denied");
        }
        
        return tableRepository.findByDiagramIdWithColumns(diagramId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Helper methods
    private DiagramTable findTableOrThrow(String tableId) {
        return tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table not found: " + tableId));
    }
    
    private TableResponse mapToResponse(DiagramTable table) {
        return TableResponse.builder()
            .id(table.getId())
            .diagramId(table.getDiagram().getId())
            .name(table.getName())
            .displayName(table.getDisplayName())
            .positionX(table.getPositionX())
            .positionY(table.getPositionY())
            .width(table.getWidth())
            .height(table.getHeight())
            .color(table.getColor())
            .isCollapsed(table.getIsCollapsed())
            .isLocked(table.getIsLocked())
            .columns(table.getColumns().stream()
                .map(this::mapColumnToResponse)
                .collect(Collectors.toList()))
            .createdAt(table.getCreatedAt())
            .updatedAt(table.getUpdatedAt())
            .build();
    }
    
    private TableResponse.ColumnDTO mapColumnToResponse(TableColumn column) {
        return TableResponse.ColumnDTO.builder()
            .id(column.getId())
            .name(column.getName())
            .dataType(column.getDataType())
            .isPrimaryKey(column.getIsPrimaryKey())
            .isForeignKey(column.getIsForeignKey())
            .isNullable(column.getIsNullable())
            .isUnique(column.getIsUnique())
            .defaultValue(column.getDefaultValue())
            .orderIndex(column.getOrderIndex())
            .build();
    }
}
```

---

### 4. ColumnService

```java
package com.chartdb.service;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.request.UpdateColumnRequest;
import com.chartdb.dto.response.ColumnResponse;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ForbiddenException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.DiagramTable;
import com.chartdb.model.TableColumn;
import com.chartdb.repository.ColumnRepository;
import com.chartdb.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnService {
    
    private final ColumnRepository columnRepository;
    private final TableRepository tableRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    
    @Transactional
    public ColumnResponse createColumn(CreateColumnRequest request, String userId) {
        DiagramTable table = tableRepository.findById(request.getTableId())
            .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        
        if (!permissionService.canEdit(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        // Check unique name
        if (columnRepository.existsByTableIdAndName(request.getTableId(), request.getName())) {
            throw new BadRequestException("Column name already exists in this table");
        }
        
        // Get next order index
        Integer nextIndex = columnRepository.getMaxOrderIndex(request.getTableId()) + 1;
        
        TableColumn column = TableColumn.builder()
            .id(UUID.randomUUID().toString())
            .table(table)
            .name(request.getName())
            .dataType(request.getDataType())
            .isPrimaryKey(request.getIsPrimaryKey() != null ? request.getIsPrimaryKey() : false)
            .isForeignKey(request.getIsForeignKey() != null ? request.getIsForeignKey() : false)
            .isNullable(request.getIsNullable() != null ? request.getIsNullable() : true)
            .isUnique(request.getIsUnique() != null ? request.getIsUnique() : false)
            .defaultValue(request.getDefaultValue())
            .orderIndex(nextIndex)
            .build();
        
        column = columnRepository.save(column);
        log.info("Column created: {} in table {} by user {}", column.getId(), request.getTableId(), userId);
        
        // Notify collaborators
        notificationService.broadcastColumnCreated(table.getDiagram().getId(), column, userId);
        
        return mapToResponse(column);
    }
    
    @Transactional
    public ColumnResponse updateColumn(String columnId, UpdateColumnRequest request, String userId) {
        TableColumn column = findColumnOrThrow(columnId);
        
        if (!permissionService.canEdit(column.getTable().getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        // Check unique name if changing
        if (request.getName() != null && !request.getName().equals(column.getName())) {
            if (columnRepository.existsByTableIdAndNameAndIdNot(
                    column.getTable().getId(), request.getName(), columnId)) {
                throw new BadRequestException("Column name already exists");
            }
            column.setName(request.getName());
        }
        
        if (request.getDataType() != null) column.setDataType(request.getDataType());
        if (request.getIsPrimaryKey() != null) column.setIsPrimaryKey(request.getIsPrimaryKey());
        if (request.getIsForeignKey() != null) column.setIsForeignKey(request.getIsForeignKey());
        if (request.getIsNullable() != null) column.setIsNullable(request.getIsNullable());
        if (request.getIsUnique() != null) column.setIsUnique(request.getIsUnique());
        if (request.getDefaultValue() != null) column.setDefaultValue(request.getDefaultValue());
        
        column = columnRepository.save(column);
        log.info("Column updated: {} by user {}", columnId, userId);
        
        // Notify
        notificationService.broadcastColumnUpdated(column.getTable().getDiagram().getId(), column, userId);
        
        return mapToResponse(column);
    }
    
    @Transactional
    public void deleteColumn(String columnId, String userId) {
        TableColumn column = findColumnOrThrow(columnId);
        
        if (!permissionService.canEdit(column.getTable().getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        String diagramId = column.getTable().getDiagram().getId();
        String tableId = column.getTable().getId();
        Integer removedIndex = column.getOrderIndex();
        
        columnRepository.delete(column);
        
        // Shift remaining columns
        columnRepository.shiftOrderIndicesDown(tableId, removedIndex);
        
        log.info("Column deleted: {} by user {}", columnId, userId);
        
        // Notify
        notificationService.broadcastColumnDeleted(diagramId, tableId, columnId, userId);
    }
    
    @Transactional
    public void reorderColumns(String tableId, List<String> columnIds, String userId) {
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        
        if (!permissionService.canEdit(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Edit access denied");
        }
        
        for (int i = 0; i < columnIds.size(); i++) {
            columnRepository.updateOrderIndex(columnIds.get(i), i);
        }
        
        log.info("Columns reordered in table {} by user {}", tableId, userId);
    }
    
    @Transactional(readOnly = true)
    public List<ColumnResponse> getColumnsByTable(String tableId, String userId) {
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        
        if (!permissionService.canView(table.getDiagram().getId(), userId)) {
            throw new ForbiddenException("Access denied");
        }
        
        return columnRepository.findByTableIdOrderByOrderIndexAsc(tableId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private TableColumn findColumnOrThrow(String columnId) {
        return columnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("Column not found: " + columnId));
    }
    
    private ColumnResponse mapToResponse(TableColumn column) {
        return ColumnResponse.builder()
            .id(column.getId())
            .tableId(column.getTable().getId())
            .name(column.getName())
            .dataType(column.getDataType())
            .isPrimaryKey(column.getIsPrimaryKey())
            .isForeignKey(column.getIsForeignKey())
            .isNullable(column.getIsNullable())
            .isUnique(column.getIsUnique())
            .isAutoIncrement(column.getIsAutoIncrement())
            .defaultValue(column.getDefaultValue())
            .orderIndex(column.getOrderIndex())
            .build();
    }
}
```

---

### 5. PermissionService

```java
package com.chartdb.service;

import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramPermission;
import com.chartdb.model.enums.PermissionLevel;
import com.chartdb.repository.DiagramPermissionRepository;
import com.chartdb.repository.DiagramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final DiagramRepository diagramRepository;
    private final DiagramPermissionRepository permissionRepository;
    
    @Transactional(readOnly = true)
    public boolean canView(String diagramId, String userId) {
        // Check if user is owner
        Optional<Diagram> diagram = diagramRepository.findById(diagramId);
        if (diagram.isPresent()) {
            if (diagram.get().getOwner().getId().equals(userId)) {
                return true;
            }
            if (diagram.get().getIsPublic()) {
                return true;
            }
        }
        
        // Check permissions
        return permissionRepository.canUserView(diagramId, userId);
    }
    
    @Transactional(readOnly = true)
    public boolean canEdit(String diagramId, String userId) {
        // Check if user is owner
        Optional<Diagram> diagram = diagramRepository.findById(diagramId);
        if (diagram.isPresent() && diagram.get().getOwner().getId().equals(userId)) {
            return true;
        }
        
        // Check permissions
        return permissionRepository.canUserEdit(diagramId, userId);
    }
    
    @Transactional(readOnly = true)
    public boolean isOwner(String diagramId, String userId) {
        Optional<Diagram> diagram = diagramRepository.findById(diagramId);
        return diagram.isPresent() && diagram.get().getOwner().getId().equals(userId);
    }
    
    @Transactional(readOnly = true)
    public PermissionLevel getPermissionLevel(String diagramId, String userId) {
        Optional<Diagram> diagram = diagramRepository.findById(diagramId);
        if (diagram.isPresent() && diagram.get().getOwner().getId().equals(userId)) {
            return PermissionLevel.OWNER;
        }
        
        return permissionRepository.findByDiagramIdAndUserId(diagramId, userId)
            .map(DiagramPermission::getPermissionLevel)
            .orElse(null);
    }
}
```

---

### 6. NotificationService

```java
package com.chartdb.service;

import com.chartdb.model.DiagramTable;
import com.chartdb.model.Relationship;
import com.chartdb.model.TableColumn;
import com.chartdb.websocket.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final String TOPIC_PREFIX = "/topic/diagram/";
    
    // Table notifications
    @Async
    public void broadcastTableCreated(String diagramId, DiagramTable table, String userId) {
        TableCreateMessage message = TableCreateMessage.builder()
            .diagramId(diagramId)
            .tableId(table.getId())
            .tableName(table.getName())
            .positionX(table.getPositionX().doubleValue())
            .positionY(table.getPositionY().doubleValue())
            .color(table.getColor())
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "table-created", message);
    }
    
    @Async
    public void broadcastTableUpdated(String diagramId, DiagramTable table, String userId) {
        TableUpdateMessage message = TableUpdateMessage.builder()
            .diagramId(diagramId)
            .tableId(table.getId())
            .tableName(table.getName())
            .color(table.getColor())
            .isCollapsed(table.getIsCollapsed())
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "table-updated", message);
    }
    
    @Async
    public void broadcastTableMoved(String diagramId, String tableId, double x, double y, String userId) {
        TableMoveMessage message = TableMoveMessage.builder()
            .diagramId(diagramId)
            .tableId(tableId)
            .x(x)
            .y(y)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "table-moved", message);
    }
    
    @Async
    public void broadcastTableDeleted(String diagramId, String tableId, String userId) {
        TableDeleteMessage message = TableDeleteMessage.builder()
            .diagramId(diagramId)
            .tableId(tableId)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "table-deleted", message);
    }
    
    // Column notifications
    @Async
    public void broadcastColumnCreated(String diagramId, TableColumn column, String userId) {
        ColumnMessage message = ColumnMessage.builder()
            .diagramId(diagramId)
            .tableId(column.getTable().getId())
            .columnId(column.getId())
            .columnName(column.getName())
            .dataType(column.getDataType())
            .action("created")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "column-created", message);
    }
    
    @Async
    public void broadcastColumnUpdated(String diagramId, TableColumn column, String userId) {
        ColumnMessage message = ColumnMessage.builder()
            .diagramId(diagramId)
            .tableId(column.getTable().getId())
            .columnId(column.getId())
            .columnName(column.getName())
            .dataType(column.getDataType())
            .action("updated")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "column-updated", message);
    }
    
    @Async
    public void broadcastColumnDeleted(String diagramId, String tableId, String columnId, String userId) {
        ColumnMessage message = ColumnMessage.builder()
            .diagramId(diagramId)
            .tableId(tableId)
            .columnId(columnId)
            .action("deleted")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "column-deleted", message);
    }
    
    // Cursor notifications
    @Async
    public void broadcastCursorMove(String diagramId, String userId, String userName, 
                                     double x, double y, String color) {
        CursorMoveMessage message = CursorMoveMessage.builder()
            .diagramId(diagramId)
            .userId(userId)
            .userName(userName)
            .x(x)
            .y(y)
            .color(color)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "cursor-update", message);
    }
    
    // User presence notifications
    @Async
    public void broadcastUserJoined(String diagramId, String userId, String userName, 
                                     String avatarUrl, String cursorColor) {
        JoinDiagramMessage message = JoinDiagramMessage.builder()
            .diagramId(diagramId)
            .userId(userId)
            .userName(userName)
            .avatarUrl(avatarUrl)
            .cursorColor(cursorColor)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "user-joined", message);
    }
    
    @Async
    public void broadcastUserLeft(String diagramId, String userId) {
        LeaveDiagramMessage message = LeaveDiagramMessage.builder()
            .diagramId(diagramId)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
        
        send(diagramId, "user-left", message);
    }
    
    // Generic send method
    private void send(String diagramId, String eventType, Object message) {
        String destination = TOPIC_PREFIX + diagramId + "/" + eventType;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcast to {}: {}", destination, message);
    }
}
```

---

### 7. CollaborationService

```java
package com.chartdb.service;

import com.chartdb.dto.response.CollaboratorResponse;
import com.chartdb.model.ActiveCollaborator;
import com.chartdb.model.User;
import com.chartdb.repository.ActiveCollaboratorRepository;
import com.chartdb.repository.DiagramRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {
    
    private final ActiveCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final DiagramRepository diagramRepository;
    private final NotificationService notificationService;
    
    @Transactional
    public ActiveCollaborator joinDiagram(String diagramId, String userId, String sessionId, 
                                           String websocketSessionId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if already in diagram with same session
        var existing = collaboratorRepository.findByDiagramIdAndUserIdAndSessionId(
            diagramId, userId, sessionId);
        if (existing.isPresent()) {
            ActiveCollaborator collab = existing.get();
            collab.setIsActive(true);
            collab.setLastSeen(Instant.now());
            collab.setWebsocketSessionId(websocketSessionId);
            return collaboratorRepository.save(collab);
        }
        
        var diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new RuntimeException("Diagram not found"));
        
        ActiveCollaborator collaborator = ActiveCollaborator.builder()
            .id(UUID.randomUUID().toString())
            .diagram(diagram)
            .user(user)
            .sessionId(sessionId)
            .websocketSessionId(websocketSessionId)
            .cursorColor(user.getCursorColor())
            .userName(user.getEffectiveDisplayName())
            .userAvatar(user.getAvatarUrl())
            .isActive(true)
            .connectedAt(Instant.now())
            .lastSeen(Instant.now())
            .build();
        
        collaborator = collaboratorRepository.save(collaborator);
        
        // Notify others
        notificationService.broadcastUserJoined(
            diagramId, userId, user.getEffectiveDisplayName(),
            user.getAvatarUrl(), user.getCursorColor()
        );
        
        log.info("User {} joined diagram {}", userId, diagramId);
        return collaborator;
    }
    
    @Transactional
    public void leaveDiagram(String diagramId, String userId, String sessionId) {
        collaboratorRepository.findByDiagramIdAndUserIdAndSessionId(diagramId, userId, sessionId)
            .ifPresent(collab -> {
                collaboratorRepository.delete(collab);
                notificationService.broadcastUserLeft(diagramId, userId);
                log.info("User {} left diagram {}", userId, diagramId);
            });
    }
    
    @Transactional
    public void leaveByWebsocketSession(String websocketSessionId) {
        collaboratorRepository.findByWebsocketSessionId(websocketSessionId)
            .ifPresent(collab -> {
                String diagramId = collab.getDiagram().getId();
                String userId = collab.getUser().getId();
                collaboratorRepository.delete(collab);
                notificationService.broadcastUserLeft(diagramId, userId);
                log.info("User {} disconnected from diagram {}", userId, diagramId);
            });
    }
    
    @Transactional
    public void updateCursor(String diagramId, String userId, double x, double y) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(collab -> {
                collab.updateCursor(BigDecimal.valueOf(x), BigDecimal.valueOf(y));
                collaboratorRepository.save(collab);
            });
    }
    
    @Transactional(readOnly = true)
    public List<CollaboratorResponse> getActiveCollaborators(String diagramId) {
        return collaboratorRepository.findByDiagramIdAndIsActiveTrue(diagramId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long getActiveCollaboratorCount(String diagramId) {
        return collaboratorRepository.countByDiagramIdAndIsActiveTrue(diagramId);
    }
    
    // Scheduled cleanup of stale sessions
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cleanupStaleSessions() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        collaboratorRepository.deleteStaleSessions(threshold);
        log.debug("Cleaned up stale collaboration sessions older than {}", threshold);
    }
    
    private CollaboratorResponse mapToResponse(ActiveCollaborator collaborator) {
        return CollaboratorResponse.builder()
            .userId(collaborator.getUser().getId())
            .userName(collaborator.getUserName())
            .avatarUrl(collaborator.getUserAvatar())
            .cursorColor(collaborator.getCursorColor())
            .cursorX(collaborator.getCursorX() != null ? collaborator.getCursorX().doubleValue() : null)
            .cursorY(collaborator.getCursorY() != null ? collaborator.getCursorY().doubleValue() : null)
            .isIdle(collaborator.getIsIdle())
            .connectedAt(collaborator.getConnectedAt())
            .build();
    }
}
```

---

**← Previous:** `06-BACKEND-REPOSITORIES.md` | **Next:** `08-BACKEND-CONTROLLERS.md` →
