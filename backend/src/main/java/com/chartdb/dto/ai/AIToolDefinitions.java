package com.chartdb.dto.ai;

import java.util.List;
import java.util.Map;

/**
 * Definitions of tools/functions that AI can call to modify diagrams
 */
public class AIToolDefinitions {
    
    public static List<AITool> getDiagramTools() {
        return List.of(
            createTable(),
            addColumn(),
            modifyColumn(),
            deleteTable(),
            deleteColumn(),
            createRelationship(),
            deleteRelationship(),
            addIndex()
        );
    }
    
    private static AITool createTable() {
        return AITool.create(
            "create_table",
            "Creates a new table in the diagram with specified columns",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "name", Map.of(
                        "type", "string",
                        "description", "Name of the table"
                    ),
                    "columns", Map.of(
                        "type", "array",
                        "description", "Array of column definitions",
                        "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "name", Map.of("type", "string"),
                                "dataType", Map.of("type", "string"),
                                "nullable", Map.of("type", "boolean"),
                                "primaryKey", Map.of("type", "boolean"),
                                "unique", Map.of("type", "boolean")
                            )
                        )
                    ),
                    "description", Map.of(
                        "type", "string",
                        "description", "Optional description of the table"
                    )
                ),
                "required", List.of("name", "columns")
            )
        );
    }
    
    private static AITool addColumn() {
        return AITool.create(
            "add_column",
            "Adds a new column to an existing table",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "tableName", Map.of(
                        "type", "string",
                        "description", "Name of the table to add column to"
                    ),
                    "columnName", Map.of(
                        "type", "string",
                        "description", "Name of the new column"
                    ),
                    "dataType", Map.of(
                        "type", "string",
                        "description", "Data type (e.g., VARCHAR, INT, TIMESTAMP)"
                    ),
                    "nullable", Map.of(
                        "type", "boolean",
                        "description", "Whether the column can be null"
                    ),
                    "primaryKey", Map.of(
                        "type", "boolean",
                        "description", "Whether this is a primary key"
                    ),
                    "unique", Map.of(
                        "type", "boolean",
                        "description", "Whether values must be unique"
                    )
                ),
                "required", List.of("tableName", "columnName", "dataType")
            )
        );
    }
    
    private static AITool modifyColumn() {
        return AITool.create(
            "modify_column",
            "Modifies an existing column's properties",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "tableName", Map.of(
                        "type", "string",
                        "description", "Name of the table"
                    ),
                    "columnName", Map.of(
                        "type", "string",
                        "description", "Name of the column to modify"
                    ),
                    "newDataType", Map.of(
                        "type", "string",
                        "description", "New data type (optional)"
                    ),
                    "newName", Map.of(
                        "type", "string",
                        "description", "New column name (optional)"
                    ),
                    "nullable", Map.of(
                        "type", "boolean",
                        "description", "Whether the column can be null"
                    )
                ),
                "required", List.of("tableName", "columnName")
            )
        );
    }
    
    private static AITool deleteTable() {
        return AITool.create(
            "delete_table",
            "Deletes a table from the diagram",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "tableName", Map.of(
                        "type", "string",
                        "description", "Name of the table to delete"
                    )
                ),
                "required", List.of("tableName")
            )
        );
    }
    
    private static AITool deleteColumn() {
        return AITool.create(
            "delete_column",
            "Deletes a column from a table",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "tableName", Map.of(
                        "type", "string",
                        "description", "Name of the table"
                    ),
                    "columnName", Map.of(
                        "type", "string",
                        "description", "Name of the column to delete"
                    )
                ),
                "required", List.of("tableName", "columnName")
            )
        );
    }
    
    private static AITool createRelationship() {
        return AITool.create(
            "create_relationship",
            "Creates a relationship between two tables",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "sourceTable", Map.of(
                        "type", "string",
                        "description", "Name of the source table"
                    ),
                    "sourceColumn", Map.of(
                        "type", "string",
                        "description", "Name of the source column"
                    ),
                    "targetTable", Map.of(
                        "type", "string",
                        "description", "Name of the target table"
                    ),
                    "targetColumn", Map.of(
                        "type", "string",
                        "description", "Name of the target column"
                    ),
                    "relationshipType", Map.of(
                        "type", "string",
                        "description", "Type: ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY",
                        "enum", List.of("ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE", "MANY_TO_MANY")
                    )
                ),
                "required", List.of("sourceTable", "targetTable", "relationshipType")
            )
        );
    }
    
    private static AITool deleteRelationship() {
        return AITool.create(
            "delete_relationship",
            "Deletes a relationship between tables",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "sourceTable", Map.of(
                        "type", "string",
                        "description", "Name of the source table"
                    ),
                    "targetTable", Map.of(
                        "type", "string",
                        "description", "Name of the target table"
                    )
                ),
                "required", List.of("sourceTable", "targetTable")
            )
        );
    }
    
    private static AITool addIndex() {
        return AITool.create(
            "add_index",
            "Adds an index to a table",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "tableName", Map.of(
                        "type", "string",
                        "description", "Name of the table"
                    ),
                    "indexName", Map.of(
                        "type", "string",
                        "description", "Name of the index"
                    ),
                    "columns", Map.of(
                        "type", "array",
                        "description", "List of column names to index",
                        "items", Map.of("type", "string")
                    ),
                    "unique", Map.of(
                        "type", "boolean",
                        "description", "Whether the index should enforce uniqueness"
                    )
                ),
                "required", List.of("tableName", "indexName", "columns")
            )
        );
    }
}
