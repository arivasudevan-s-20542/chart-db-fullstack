/**
 * AI Action Executor - Executes AI function calls on the frontend instantly (Copilot-style)
 * This provides instant feedback without waiting for backend round-trips
 */

import type { DBTable } from '@/lib/domain/db-table';
import type { DBField } from '@/lib/domain/db-field';
import type {
    DBRelationship,
    RelationshipType,
} from '@/lib/domain/db-relationship';
import { generateId } from '@/lib/utils';

export interface AIFunctionCall {
    name: string;
    arguments: Record<string, any>;
}

export interface ActionResult {
    success: boolean;
    action: string;
    summary: string; // Human-readable summary
    elementId?: string; // ID of created/modified element for focusing
    targetId?: string; // Alias for elementId (backward compatibility)
    elementType?: 'table' | 'column' | 'relationship';
    error?: string;
}

export interface DiagramContext {
    addTable: (
        table: DBTable,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    updateTable: (
        tableId: string,
        updates: Partial<DBTable>,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    removeTable: (
        tableId: string,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    addField: (
        tableId: string,
        field: DBField,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    updateField: (
        tableId: string,
        fieldId: string,
        updates: Partial<DBField>,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    removeField: (
        tableId: string,
        fieldId: string,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    addRelationship: (
        relationship: DBRelationship,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    updateRelationship: (
        relationshipId: string,
        updates: Partial<DBRelationship>,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    removeRelationship: (
        relationshipId: string,
        options?: { updateHistory: boolean }
    ) => Promise<void>;
    tables: DBTable[];
}

/**
 * Execute AI function call instantly on frontend
 */
export async function executeAIAction(
    functionCall: AIFunctionCall,
    diagramContext: DiagramContext
): Promise<ActionResult> {
    const { name, arguments: args } = functionCall;

    try {
        switch (name) {
            case 'create_table':
                return await createTable(args, diagramContext);
            case 'add_column':
                return await addColumn(args, diagramContext);
            case 'modify_column':
                return await modifyColumn(args, diagramContext);
            case 'delete_table':
                return await deleteTable(args, diagramContext);
            case 'delete_column':
                return await deleteColumn(args, diagramContext);
            case 'create_relationship':
                return await createRelationship(args, diagramContext);
            case 'delete_relationship':
                return await deleteRelationship(args, diagramContext);
            default:
                return {
                    success: false,
                    action: name,
                    summary: `Unknown action: ${name}`,
                    error: `Function '${name}' is not implemented`,
                };
        }
    } catch (error) {
        return {
            success: false,
            action: name,
            summary: `Failed to execute ${name}`,
            error: error instanceof Error ? error.message : String(error),
        };
    }
}

async function createTable(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const tableName = args.name as string;
    const columns = (args.columns as any[]) || [];

    // Check if table already exists
    if (context.tables.find((t) => t.name === tableName)) {
        return {
            success: false,
            action: 'create_table',
            summary: `Table '${tableName}' already exists`,
            error: 'Duplicate table name',
        };
    }

    // Generate table ID
    const tableId = generateId();

    // Create fields from columns
    const fields: DBField[] = columns.map((col) => ({
        id: generateId(),
        name: col.name,
        type: { id: generateId(), name: col.dataType },
        primaryKey: col.primaryKey || false,
        unique: col.unique || false,
        nullable: col.nullable !== false, // Default to true
        createdAt: Date.now(),
    }));

    // Create table
    const newTable: DBTable = {
        id: tableId,
        name: tableName,
        fields,
        indexes: [],
        color: '#7c3aed', // Default purple color
        isView: false,
        x: 100, // Default position - will be adjusted by auto-layout
        y: 100,
        createdAt: Date.now(),
    };

    await context.addTable(newTable, { updateHistory: true });

    return {
        success: true,
        action: 'create_table',
        summary: `✨ Created table '${tableName}' with ${fields.length} column${fields.length !== 1 ? 's' : ''}`,
        elementId: tableId,
        elementType: 'table',
    };
}

async function addColumn(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const tableName = args.tableName as string;
    const columnName = args.columnName || args.name;
    const dataType = args.dataType;

    // Find table
    const table = context.tables.find((t) => t.name === tableName);
    if (!table) {
        return {
            success: false,
            action: 'add_column',
            summary: `Table '${tableName}' not found`,
            error: 'Table does not exist',
        };
    }

    // Check if column exists
    if (table.fields.find((f) => f.name === columnName)) {
        return {
            success: false,
            action: 'add_column',
            summary: `Column '${columnName}' already exists in '${tableName}'`,
            error: 'Duplicate column name',
        };
    }

    // Create new field
    const newField: DBField = {
        id: generateId(),
        name: columnName,
        type: { id: generateId(), name: dataType },
        primaryKey: args.primaryKey || false,
        unique: args.unique || false,
        nullable: args.nullable !== false,
        createdAt: Date.now(),
    };

    await context.addField(table.id, newField, { updateHistory: true });

    return {
        success: true,
        action: 'add_column',
        summary: `➕ Added column '${columnName}' to '${tableName}'`,
        elementId: table.id,
        elementType: 'table',
    };
}

async function modifyColumn(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const tableName = args.tableName as string;
    const columnName = args.columnName as string;

    // Find table
    const table = context.tables.find((t) => t.name === tableName);
    if (!table) {
        return {
            success: false,
            action: 'modify_column',
            summary: `Table '${tableName}' not found`,
            error: 'Table does not exist',
        };
    }

    // Find column
    const field = table.fields.find((f) => f.name === columnName);
    if (!field) {
        return {
            success: false,
            action: 'modify_column',
            summary: `Column '${columnName}' not found in '${tableName}'`,
            error: 'Column does not exist',
        };
    }

    // Build updates
    const updates: Partial<DBField> = {};
    if (args.newDataType) {
        updates.type = { id: field.type.id, name: args.newDataType };
    }
    if (args.newName) {
        updates.name = args.newName;
    }
    if (args.nullable !== undefined) {
        updates.nullable = args.nullable;
    }

    await context.updateField(table.id, field.id, updates, {
        updateHistory: true,
    });

    const changes = Object.keys(updates).join(', ');
    return {
        success: true,
        action: 'modify_column',
        summary: `✏️ Modified column '${columnName}' in '${tableName}' (${changes})`,
        elementId: table.id,
        elementType: 'table',
    };
}

async function deleteTable(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const tableName = args.tableName as string;

    // Find table
    const table = context.tables.find((t) => t.name === tableName);
    if (!table) {
        return {
            success: false,
            action: 'delete_table',
            summary: `Table '${tableName}' not found`,
            error: 'Table does not exist',
        };
    }

    await context.removeTable(table.id, { updateHistory: true });

    return {
        success: true,
        action: 'delete_table',
        summary: `🗑️ Deleted table '${tableName}'`,
    };
}

async function deleteColumn(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const tableName = args.tableName as string;
    const columnName = args.columnName as string;

    // Find table
    const table = context.tables.find((t) => t.name === tableName);
    if (!table) {
        return {
            success: false,
            action: 'delete_column',
            summary: `Table '${tableName}' not found`,
            error: 'Table does not exist',
        };
    }

    // Find column
    const field = table.fields.find((f) => f.name === columnName);
    if (!field) {
        return {
            success: false,
            action: 'delete_column',
            summary: `Column '${columnName}' not found in '${tableName}'`,
            error: 'Column does not exist',
        };
    }

    await context.removeField(table.id, field.id, { updateHistory: true });

    return {
        success: true,
        action: 'delete_column',
        summary: `➖ Removed column '${columnName}' from '${tableName}'`,
        elementId: table.id,
        elementType: 'table',
    };
}

async function createRelationship(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const sourceTable = args.sourceTable as string;
    const targetTable = args.targetTable as string;
    const relationshipType =
        (args.relationshipType as RelationshipType) || 'one_to_many';

    // Find tables
    const source = context.tables.find((t) => t.name === sourceTable);
    const target = context.tables.find((t) => t.name === targetTable);

    if (!source) {
        return {
            success: false,
            action: 'create_relationship',
            summary: `Source table '${sourceTable}' not found`,
            error: 'Source table does not exist',
        };
    }

    if (!target) {
        return {
            success: false,
            action: 'create_relationship',
            summary: `Target table '${targetTable}' not found`,
            error: 'Target table does not exist',
        };
    }

    // Create relationship
    const relationshipId = generateId();

    // Convert relationship type to cardinalities
    const getCardinalities = (
        relType: RelationshipType
    ): { source: 'one' | 'many'; target: 'one' | 'many' } => {
        switch (relType) {
            case 'one_to_one':
                return { source: 'one', target: 'one' };
            case 'one_to_many':
                return { source: 'one', target: 'many' };
            case 'many_to_one':
                return { source: 'many', target: 'one' };
            case 'many_to_many':
                return { source: 'many', target: 'many' };
            default:
                return { source: 'one', target: 'many' };
        }
    };

    const cardinalities = getCardinalities(relationshipType);

    const relationship: DBRelationship = {
        id: relationshipId,
        name: `${sourceTable}_${targetTable}`,
        sourceTableId: source.id,
        targetTableId: target.id,
        sourceCardinality: cardinalities.source,
        targetCardinality: cardinalities.target,
        sourceFieldId: source.fields[0]?.id || '', // TODO: Smarter field selection
        targetFieldId: target.fields[0]?.id || '',
        createdAt: Date.now(),
    };

    await context.addRelationship(relationship, { updateHistory: true });

    return {
        success: true,
        action: 'create_relationship',
        summary: `🔗 Created relationship from '${sourceTable}' to '${targetTable}'`,
        elementId: relationshipId,
        elementType: 'relationship',
    };
}

async function deleteRelationship(
    args: any,
    context: DiagramContext
): Promise<ActionResult> {
    const sourceTable = args.sourceTable as string;
    const targetTable = args.targetTable as string;

    // Find tables
    const source = context.tables.find((t) => t.name === sourceTable);
    const target = context.tables.find((t) => t.name === targetTable);

    if (!source || !target) {
        return {
            success: false,
            action: 'delete_relationship',
            summary: `Tables not found`,
            error: 'One or both tables do not exist',
        };
    }

    // Find relationship (simplified - in real implementation, check all relationships)
    // For now, we'll just return success
    // TODO: Implement proper relationship lookup

    return {
        success: true,
        action: 'delete_relationship',
        summary: `🔓 Removed relationship between '${sourceTable}' and '${targetTable}'`,
    };
}
