package com.chartdb.service;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.dto.request.MoveTableRequest;
import com.chartdb.dto.request.UpdateTableRequest;
import com.chartdb.dto.response.TableResponse;
import com.chartdb.exception.AccessDeniedException;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.ColumnMapper;
import com.chartdb.mapper.TableMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramTable;
import com.chartdb.model.TableColumn;
import com.chartdb.repository.ColumnRepository;
import com.chartdb.repository.RelationshipRepository;
import com.chartdb.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {
    
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final RelationshipRepository relationshipRepository;
    private final DiagramService diagramService;
    private final TableMapper tableMapper;
    private final ColumnMapper columnMapper;
    
    @Transactional
    public TableResponse createTable(String diagramId, String userId, CreateTableRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this diagram");
        }
        
        if (tableRepository.existsByDiagramIdAndName(diagramId, request.getName())) {
            throw new BadRequestException("Table with name '" + request.getName() + "' already exists");
        }
        
        DiagramTable table = tableMapper.toEntity(request);
        table.setDiagram(diagram);
        table.setSortOrder(tableRepository.getMaxSortOrder(diagramId) + 1);
        table.setZIndex(tableRepository.getMaxZIndex(diagramId) + 1);
        table.setIsHidden(false);
        
        if (table.getPositionX() == null) table.setPositionX(BigDecimal.valueOf(100));
        if (table.getPositionY() == null) table.setPositionY(BigDecimal.valueOf(100));
        if (table.getWidth() == null) table.setWidth(BigDecimal.valueOf(200));
        if (table.getHeight() == null) table.setHeight(BigDecimal.valueOf(150));
        if (table.getIsCollapsed() == null) table.setIsCollapsed(false);
        
        table = tableRepository.save(table);
        
        // Create columns if provided
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            int orderIndex = 0;
            for (CreateColumnRequest columnRequest : request.getColumns()) {
                TableColumn column = columnMapper.toEntity(columnRequest);
                column.setTable(table);
                if (column.getOrderIndex() == null) {
                    column.setOrderIndex(orderIndex++);
                }
                columnRepository.save(column);
                log.debug("Column created: {} in table {}", column.getId(), table.getId());
            }
        }
        
        log.info("Table created: {} in diagram {} by user {}", table.getId(), diagramId, userId);
        
        // Reload table with columns
        table = tableRepository.findByIdWithColumns(table.getId()).orElse(table);
        return tableMapper.toResponse(table);
    }
    
    @Transactional(readOnly = true)
    public List<TableResponse> getDiagramTables(String diagramId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<DiagramTable> tables = tableRepository.findByDiagramIdWithColumns(diagramId);
        return tableMapper.toResponseList(tables);
    }
    
    @Transactional(readOnly = true)
    public TableResponse getTable(String tableId, String userId) {
        DiagramTable table = findTableById(tableId);
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this table");
        }
        
        return tableMapper.toResponse(table);
    }
    
    @Transactional
    public TableResponse updateTable(String tableId, String userId, UpdateTableRequest request) {
        DiagramTable table = findTableById(tableId);
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this table");
        }
        
        if (request.getName() != null && !request.getName().equals(table.getName())) {
            if (tableRepository.existsByDiagramIdAndNameAndIdNot(diagram.getId(), request.getName(), tableId)) {
                throw new BadRequestException("Table with name '" + request.getName() + "' already exists");
            }
            table.setName(request.getName());
        }
        
        // Handle schema - check both fields
        String schema = request.getEffectiveSchema();
        if (schema != null) table.setSchemaName(schema);
        
        // Handle description/comment - check both fields
        String comment = request.getEffectiveComment();
        if (comment != null) table.setDescription(comment);
        
        if (request.getPositionX() != null) table.setPositionX(request.getPositionX());
        if (request.getPositionY() != null) table.setPositionY(request.getPositionY());
        if (request.getWidth() != null) table.setWidth(request.getWidth());
        if (request.getHeight() != null) table.setHeight(request.getHeight());
        if (request.getColor() != null) table.setColor(request.getColor());
        if (request.getIsCollapsed() != null) table.setIsCollapsed(request.getIsCollapsed());
        if (request.getIsHidden() != null) table.setIsHidden(request.getIsHidden());
        if (request.getZIndex() != null) table.setZIndex(request.getZIndex());
        if (request.getIsView() != null) table.setIsView(request.getIsView());
        if (request.getIsMaterializedView() != null) table.setIsMaterializedView(request.getIsMaterializedView());
        
        // Handle indexes JSON
        if (request.getIndexes() != null) {
            table.setIndexesJson(request.getIndexes());
        }
        
        // Handle columns - sync with incoming columns
        if (request.getColumns() != null) {
            syncColumns(table, request.getColumns());
        }
        
        table = tableRepository.save(table);
        
        // Reload with columns
        table = findTableById(tableId);
        return tableMapper.toResponse(table);
    }
    
    /**
     * Sync columns: add new, update existing, delete removed
     */
    private void syncColumns(DiagramTable table, List<CreateColumnRequest> columnRequests) {
        // Build map of existing columns by ID
        java.util.Map<String, TableColumn> existingColumns = new java.util.HashMap<>();
        for (TableColumn col : table.getColumns()) {
            existingColumns.put(col.getId(), col);
        }
        
        // Track which columns we've processed
        java.util.Set<String> processedIds = new java.util.HashSet<>();
        
        int orderIndex = 0;
        for (CreateColumnRequest req : columnRequests) {
            String colId = req.getId();
            if (colId != null && existingColumns.containsKey(colId)) {
                // Update existing column
                TableColumn existing = existingColumns.get(colId);
                updateColumnFromRequest(existing, req, orderIndex++);
                processedIds.add(colId);
            } else {
                // Create new column
                TableColumn newColumn = columnMapper.toEntity(req);
                newColumn.setTable(table);
                newColumn.setOrderIndex(orderIndex++);
                if (newColumn.getId() == null) {
                    newColumn.setId(java.util.UUID.randomUUID().toString());
                }
                columnRepository.save(newColumn);
                table.getColumns().add(newColumn);
                if (colId != null) processedIds.add(colId);
            }
        }
        
        // Delete columns not in the request
        java.util.List<TableColumn> toRemove = new java.util.ArrayList<>();
        for (TableColumn col : table.getColumns()) {
            if (!processedIds.contains(col.getId())) {
                toRemove.add(col);
            }
        }
        for (TableColumn col : toRemove) {
            table.getColumns().remove(col);
            columnRepository.delete(col);
        }
    }
    
    private void updateColumnFromRequest(TableColumn column, CreateColumnRequest req, int orderIndex) {
        if (req.getName() != null) column.setName(req.getName());
        String dataType = req.getEffectiveDataType();
        if (dataType != null) column.setDataType(dataType);
        if (req.getIsPrimaryKey() != null) column.setIsPrimaryKey(req.getIsPrimaryKey());
        if (req.getIsForeignKey() != null) column.setIsForeignKey(req.getIsForeignKey());
        if (req.getIsNullable() != null) column.setIsNullable(req.getIsNullable());
        if (req.getIsUnique() != null) column.setIsUnique(req.getIsUnique());
        if (req.getIsAutoIncrement() != null) column.setIsAutoIncrement(req.getIsAutoIncrement());
        if (req.getDefaultValue() != null) column.setDefaultValue(req.getDefaultValue());
        if (req.getComment() != null) column.setDescription(req.getComment());
        column.setOrderIndex(orderIndex);
        columnRepository.save(column);
    }
    
    @Transactional
    public TableResponse moveTable(String tableId, String userId, MoveTableRequest request) {
        DiagramTable table = findTableById(tableId);
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this table");
        }
        
        tableRepository.updatePosition(tableId, request.getPositionX(), request.getPositionY(), Instant.now());
        
        table.setPositionX(request.getPositionX());
        table.setPositionY(request.getPositionY());
        
        return tableMapper.toResponse(table);
    }
    
    @Transactional
    public void batchMoveTables(String diagramId, String userId, List<String> tableIds, BigDecimal deltaX, BigDecimal deltaY) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this diagram");
        }
        
        tableRepository.updatePositionsByDelta(tableIds, deltaX, deltaY, Instant.now());
    }
    
    @Transactional
    public void deleteTable(String tableId, String userId) {
        DiagramTable table = findTableById(tableId);
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to delete this table");
        }
        
        // Clear FK references to this table
        columnRepository.clearForeignKeyReferences(tableId);
        
        // Delete relationships
        relationshipRepository.deleteByTableId(tableId);
        
        // Delete the table (cascades to columns)
        tableRepository.delete(table);
        log.info("Table deleted: {} from diagram {} by user {}", tableId, diagram.getId(), userId);
    }
    
    public DiagramTable findTableById(String tableId) {
        return tableRepository.findByIdWithColumns(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table", "id", tableId));
    }
}
