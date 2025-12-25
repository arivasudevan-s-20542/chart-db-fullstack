package com.chartdb.service;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.request.UpdateColumnRequest;
import com.chartdb.dto.response.ColumnResponse;
import com.chartdb.exception.AccessDeniedException;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.ColumnMapper;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnService {
    
    private final ColumnRepository columnRepository;
    private final TableRepository tableRepository;
    private final RelationshipRepository relationshipRepository;
    private final DiagramService diagramService;
    private final ColumnMapper columnMapper;
    
    @Transactional
    public ColumnResponse createColumn(String tableId, String userId, CreateColumnRequest request) {
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table", "id", tableId));
        
        Diagram diagram = table.getDiagram();
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this table");
        }
        
        if (columnRepository.existsByTableIdAndName(tableId, request.getName())) {
            throw new BadRequestException("Column with name '" + request.getName() + "' already exists");
        }
        
        TableColumn column = columnMapper.toEntity(request);
        column.setTable(table);
        
        if (column.getOrderIndex() == null) {
            column.setOrderIndex(columnRepository.getMaxOrderIndex(tableId) + 1);
        }
        
        // Handle FK references
        if (request.getFkTableId() != null) {
            DiagramTable fkTable = tableRepository.findById(request.getFkTableId())
                .orElseThrow(() -> new BadRequestException("FK target table not found"));
            column.setFkTable(fkTable);
            column.setIsForeignKey(true);
            
            if (request.getFkColumnId() != null) {
                TableColumn fkColumn = columnRepository.findById(request.getFkColumnId())
                    .orElseThrow(() -> new BadRequestException("FK target column not found"));
                column.setFkColumn(fkColumn);
            }
        }
        
        column = columnRepository.save(column);
        log.info("Column created: {} in table {} by user {}", column.getId(), tableId, userId);
        
        return columnMapper.toResponse(column);
    }
    
    @Transactional(readOnly = true)
    public List<ColumnResponse> getTableColumns(String tableId, String userId) {
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table", "id", tableId));
        
        Diagram diagram = table.getDiagram();
        if (!diagramService.canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this table");
        }
        
        List<TableColumn> columns = columnRepository.findByTableIdOrderByOrderIndexAsc(tableId);
        return columnMapper.toResponseList(columns);
    }
    
    @Transactional
    public ColumnResponse updateColumn(String columnId, String userId, UpdateColumnRequest request) {
        TableColumn column = findColumnById(columnId);
        DiagramTable table = column.getTable();
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this column");
        }
        
        if (request.getName() != null && !request.getName().equals(column.getName())) {
            if (columnRepository.existsByTableIdAndNameAndIdNot(table.getId(), request.getName(), columnId)) {
                throw new BadRequestException("Column with name '" + request.getName() + "' already exists");
            }
            column.setName(request.getName());
        }
        
        if (request.getDataType() != null) column.setDataType(request.getDataType());
        if (request.getIsPrimaryKey() != null) column.setIsPrimaryKey(request.getIsPrimaryKey());
        if (request.getIsNullable() != null) column.setIsNullable(request.getIsNullable());
        if (request.getIsUnique() != null) column.setIsUnique(request.getIsUnique());
        if (request.getIsAutoIncrement() != null) column.setIsAutoIncrement(request.getIsAutoIncrement());
        if (request.getDefaultValue() != null) column.setDefaultValue(request.getDefaultValue());
        if (request.getComment() != null) column.setDescription(request.getComment());
        if (request.getCheckConstraint() != null) column.setCheckConstraint(request.getCheckConstraint());
        if (request.getOrderIndex() != null) column.setOrderIndex(request.getOrderIndex());
        
        // Handle FK updates
        if (request.getFkTableId() != null) {
            DiagramTable fkTable = tableRepository.findById(request.getFkTableId())
                .orElseThrow(() -> new BadRequestException("FK target table not found"));
            column.setFkTable(fkTable);
            column.setIsForeignKey(true);
            
            if (request.getFkColumnId() != null) {
                TableColumn fkColumn = columnRepository.findById(request.getFkColumnId())
                    .orElseThrow(() -> new BadRequestException("FK target column not found"));
                column.setFkColumn(fkColumn);
            }
        } else if (request.getIsForeignKey() != null && !request.getIsForeignKey()) {
            column.setFkTable(null);
            column.setFkColumn(null);
            column.setIsForeignKey(false);
        }
        
        column = columnRepository.save(column);
        return columnMapper.toResponse(column);
    }
    
    @Transactional
    public void deleteColumn(String columnId, String userId) {
        TableColumn column = findColumnById(columnId);
        DiagramTable table = column.getTable();
        Diagram diagram = table.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to delete this column");
        }
        
        int orderIndex = column.getOrderIndex();
        
        // Delete relationships involving this column
        relationshipRepository.deleteByColumnId(columnId);
        
        // Delete the column
        columnRepository.delete(column);
        
        // Shift order indices
        columnRepository.shiftOrderIndicesDown(table.getId(), orderIndex);
        
        log.info("Column deleted: {} from table {} by user {}", columnId, table.getId(), userId);
    }
    
    @Transactional
    public void reorderColumns(String tableId, String userId, List<String> columnIds) {
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table", "id", tableId));
        
        Diagram diagram = table.getDiagram();
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this table");
        }
        
        for (int i = 0; i < columnIds.size(); i++) {
            columnRepository.updateOrderIndex(columnIds.get(i), i);
        }
    }
    
    public TableColumn findColumnById(String columnId) {
        return columnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
    }
}
