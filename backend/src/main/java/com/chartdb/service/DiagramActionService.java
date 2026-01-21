package com.chartdb.service;

import com.chartdb.dto.ai.AIFunctionCall;
import com.chartdb.model.*;
import com.chartdb.model.enums.RelationshipType;
import com.chartdb.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for executing AI-requested diagram actions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagramActionService {
    
    private final DiagramRepository diagramRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final RelationshipRepository relationshipRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public Map<String, Object> executeFunction(AIFunctionCall functionCall, String diagramId, String userId) {
        log.info("Executing function: {} for diagram: {}", functionCall.getName(), diagramId);
        
        try {
            return switch (functionCall.getName()) {
                case "create_table" -> createTable(functionCall.getArguments(), diagramId, userId);
                case "add_column" -> addColumn(functionCall.getArguments(), diagramId, userId);
                case "modify_column" -> modifyColumn(functionCall.getArguments(), diagramId, userId);
                case "delete_table" -> deleteTable(functionCall.getArguments(), diagramId, userId);
                case "delete_column" -> deleteColumn(functionCall.getArguments(), diagramId, userId);
                case "create_relationship" -> createRelationship(functionCall.getArguments(), diagramId, userId);
                case "delete_relationship" -> deleteRelationship(functionCall.getArguments(), diagramId, userId);
                case "add_index" -> addIndex(functionCall.getArguments(), diagramId, userId);
                default -> Map.of("success", false, "error", "Unknown function: " + functionCall.getName());
            };
        } catch (Exception e) {
            log.error("Error executing function: {}", functionCall.getName(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    private Map<String, Object> createTable(Map<String, Object> args, String diagramId, String userId) {
        String tableName = (String) args.get("name");
        String description = (String) args.get("description");
        List<Map<String, Object>> columnsData = (List<Map<String, Object>>) args.get("columns");
        
        Diagram diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new RuntimeException("Diagram not found"));
        
        // Check if table already exists
        if (tableRepository.findByDiagramIdAndName(diagramId, tableName).isPresent()) {
            return Map.of("success", false, "error", "Table '" + tableName + "' already exists");
        }
        
        // Create table
        DiagramTable table = new DiagramTable();
        table.setDiagram(diagram);
        table.setName(tableName);
        table.setDescription(description);
        // Note: Position will be set by frontend after creation
        table = tableRepository.save(table);
        
        // Create columns
        List<String> createdColumns = new ArrayList<>();
        for (Map<String, Object> colData : columnsData) {
            TableColumn column = new TableColumn();
            column.setTable(table);
            column.setName((String) colData.get("name"));
            column.setDataType((String) colData.get("dataType"));
            
            // Handle boolean conversions properly
            Object nullable = colData.get("nullable");
            column.setIsNullable(nullable instanceof Boolean ? (Boolean) nullable : Boolean.TRUE);
            
            Object primaryKey = colData.get("primaryKey");
            column.setIsPrimaryKey(primaryKey instanceof Boolean ? (Boolean) primaryKey : Boolean.FALSE);
            
            Object unique = colData.get("unique");
            column.setIsUnique(unique instanceof Boolean ? (Boolean) unique : Boolean.FALSE);
            
            columnRepository.save(column);
            createdColumns.add(column.getName());
        }
        
        return Map.of(
            "success", true,
            "message", "Created table '" + tableName + "' with " + createdColumns.size() + " columns",
            "tableId", table.getId(),
            "tableName", tableName,
            "columns", createdColumns
        );
    }
    
    private Map<String, Object> addColumn(Map<String, Object> args, String diagramId, String userId) {
        String tableName = (String) args.get("tableName");
        String columnName = (String) args.get("columnName");
        String dataType = (String) args.get("dataType");
        
        DiagramTable table = tableRepository.findByDiagramIdAndName(diagramId, tableName)
            .orElseThrow(() -> new RuntimeException("Table '" + tableName + "' not found"));
        
        // Check if column already exists
        if (columnRepository.findByTableIdAndName(table.getId(), columnName).isPresent()) {
            return Map.of("success", false, "error", "Column '" + columnName + "' already exists in table '" + tableName + "'");
        }
        
        TableColumn column = new TableColumn();
        column.setTable(table);
        column.setName(columnName);
        column.setDataType(dataType);
        
        Object nullable = args.get("nullable");
        column.setIsNullable(nullable instanceof Boolean ? (Boolean) nullable : Boolean.TRUE);
        
        Object primaryKey = args.get("primaryKey");
        column.setIsPrimaryKey(primaryKey instanceof Boolean ? (Boolean) primaryKey : Boolean.FALSE);
        
        Object unique = args.get("unique");
        column.setIsUnique(unique instanceof Boolean ? (Boolean) unique : Boolean.FALSE);
        
        columnRepository.save(column);
        
        return Map.of(
            "success", true,
            "message", "Added column '" + columnName + "' to table '" + tableName + "'",
            "tableName", tableName,
            "columnName", columnName,
            "dataType", dataType
        );
    }
    
    private Map<String, Object> modifyColumn(Map<String, Object> args, String diagramId, String userId) {
        String tableName = (String) args.get("tableName");
        String columnName = (String) args.get("columnName");
        String newDataType = (String) args.get("newDataType");
        String newName = (String) args.get("newName");
        Boolean nullable = (Boolean) args.get("nullable");
        
        DiagramTable table = tableRepository.findByDiagramIdAndName(diagramId, tableName)
            .orElseThrow(() -> new RuntimeException("Table '" + tableName + "' not found"));
        
        TableColumn column = columnRepository.findByTableIdAndName(table.getId(), columnName)
            .orElseThrow(() -> new RuntimeException("Column '" + columnName + "' not found in table '" + tableName + "'"));
        
        List<String> changes = new ArrayList<>();
        if (newDataType != null) {
            column.setDataType(newDataType);
            changes.add("dataType: " + newDataType);
        }
        if (newName != null) {
            column.setName(newName);
            changes.add("name: " + newName);
        }
        if (nullable != null) {
            column.setIsNullable(nullable);
            changes.add("nullable: " + nullable);
        }
        
        columnRepository.save(column);
        
        return Map.of(
            "success", true,
            "message", "Modified column '" + columnName + "' in table '" + tableName + "'",
            "changes", changes
        );
    }
    
    private Map<String, Object> deleteTable(Map<String, Object> args, String diagramId, String userId) {
        String tableName = (String) args.get("tableName");
        
        DiagramTable table = tableRepository.findByDiagramIdAndName(diagramId, tableName)
            .orElseThrow(() -> new RuntimeException("Table '" + tableName + "' not found"));
        
        tableRepository.delete(table);
        
        return Map.of(
            "success", true,
            "message", "Deleted table '" + tableName + "'"
        );
    }
    
    private Map<String, Object> deleteColumn(Map<String, Object> args, String diagramId, String userId) {
        String tableName = (String) args.get("tableName");
        String columnName = (String) args.get("columnName");
        
        DiagramTable table = tableRepository.findByDiagramIdAndName(diagramId, tableName)
            .orElseThrow(() -> new RuntimeException("Table '" + tableName + "' not found"));
        
        TableColumn column = columnRepository.findByTableIdAndName(table.getId(), columnName)
            .orElseThrow(() -> new RuntimeException("Column '" + columnName + "' not found"));
        
        columnRepository.delete(column);
        
        return Map.of(
            "success", true,
            "message", "Deleted column '" + columnName + "' from table '" + tableName + "'"
        );
    }
    
    private Map<String, Object> createRelationship(Map<String, Object> args, String diagramId, String userId) {
        String sourceTable = (String) args.get("sourceTable");
        String targetTable = (String) args.get("targetTable");
        String relationshipTypeStr = (String) args.get("relationshipType");
        
        DiagramTable source = tableRepository.findByDiagramIdAndName(diagramId, sourceTable)
            .orElseThrow(() -> new RuntimeException("Source table '" + sourceTable + "' not found"));
        
        DiagramTable target = tableRepository.findByDiagramIdAndName(diagramId, targetTable)
            .orElseThrow(() -> new RuntimeException("Target table '" + targetTable + "' not found"));
        
        RelationshipType type = RelationshipType.valueOf(relationshipTypeStr);
        
        Relationship relationship = new Relationship();
        relationship.setSourceTable(source);
        relationship.setTargetTable(target);
        relationship.setRelationshipType(type);
        relationshipRepository.save(relationship);
        
        return Map.of(
            "success", true,
            "message", "Created " + type + " relationship from '" + sourceTable + "' to '" + targetTable + "'",
            "sourceTable", sourceTable,
            "targetTable", targetTable,
            "type", type.toString()
        );
    }
    
    private Map<String, Object> deleteRelationship(Map<String, Object> args, String diagramId, String userId) {
        String sourceTable = (String) args.get("sourceTable");
        String targetTable = (String) args.get("targetTable");
        
        DiagramTable source = tableRepository.findByDiagramIdAndName(diagramId, sourceTable)
            .orElseThrow(() -> new RuntimeException("Source table '" + sourceTable + "' not found"));
        
        DiagramTable target = tableRepository.findByDiagramIdAndName(diagramId, targetTable)
            .orElseThrow(() -> new RuntimeException("Target table '" + targetTable + "' not found"));
        
        List<Relationship> relationships = relationshipRepository.findBetweenTables(source.getId(), target.getId());
        if (relationships.isEmpty()) {
            return Map.of("success", false, "error", "No relationship found between '" + sourceTable + "' and '" + targetTable + "'");
        }
        
        relationshipRepository.deleteAll(relationships);
        
        return Map.of(
            "success", true,
            "message", "Deleted relationship from '" + sourceTable + "' to '" + targetTable + "'"
        );
    }
    
    private Map<String, Object> addIndex(Map<String, Object> args, String diagramId, String userId) {
        // Index functionality not yet fully implemented in entity model
        // This is a placeholder for future implementation
        return Map.of(
            "success", true,
            "message", "Index support is coming soon",
            "note", "Index metadata will be added in a future update"
        );
    }
}
