import apiClient from './api-client';
import type { Diagram } from '@/lib/domain/diagram';
import type { DatabaseType } from '@/lib/domain/database-type';
import type { DatabaseEdition } from '@/lib/domain/database-edition';

// Backend wraps all responses in ApiResponse
interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

// Helper to unwrap API response
const unwrap = <T>(response: { data: ApiResponse<T> | T }): T => {
    const data = response.data as any;
    // Check if it's wrapped in ApiResponse
    if (
        data &&
        typeof data === 'object' &&
        'success' in data &&
        'data' in data
    ) {
        return data.data;
    }
    return data;
};

// Backend DTOs
export interface DiagramDTO {
    id: string;
    name: string;
    description?: string;
    databaseType: string;
    databaseEdition?: string;
    isPublic: boolean;
    tags?: string[];
    createdAt: string;
    updatedAt: string;
    ownerId: string;
    ownerEmail?: string;
}

export interface CreateDiagramRequest {
    id?: string; // Optional: frontend can provide ID
    name: string;
    description?: string;
    databaseType: string;
    databaseEdition?: string;
    isPublic?: boolean;
    tags?: string[];
}

export interface UpdateDiagramRequest {
    name?: string;
    description?: string;
    databaseType?: string;
    databaseEdition?: string;
    isPublic?: boolean;
    tags?: string[];
}

export interface DiagramListResponse {
    content: DiagramDTO[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

// Convert backend DTO to frontend Diagram type
const toDiagram = (dto: DiagramDTO): Diagram => ({
    id: dto.id,
    name: dto.name,
    databaseType: dto.databaseType as DatabaseType,
    databaseEdition: dto.databaseEdition as DatabaseEdition | undefined,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt),
    tables: [],
    relationships: [],
    dependencies: [],
    areas: [],
    customTypes: [],
    notes: [],
});

export const diagramsApi = {
    /**
     * Get all diagrams for current user
     */
    listDiagrams: async (page = 0, size = 20): Promise<DiagramListResponse> => {
        const response = await apiClient.get('/diagrams', {
            params: { page, size },
        });
        return unwrap(response);
    },

    /**
     * Get a single diagram by ID with all related data
     */
    getDiagram: async (id: string): Promise<Diagram> => {
        const response = await apiClient.get(`/diagrams/${id}`);
        const dto = unwrap<DiagramDTO>(response);
        const diagram = toDiagram(dto);

        // Load all related data in parallel
        const [tables, relationships, dependencies, areas, customTypes, notes] =
            await Promise.all([
                diagramsApi.getTables(id),
                diagramsApi.getRelationships(id),
                diagramsApi.getDependencies(id),
                diagramsApi.getAreas(id),
                diagramsApi.getCustomTypes(id),
                diagramsApi.getNotes(id),
            ]);

        return {
            ...diagram,
            tables,
            relationships,
            dependencies,
            areas,
            customTypes,
            notes,
        };
    },

    /**
     * Create a new diagram
     */
    createDiagram: async (
        request: CreateDiagramRequest
    ): Promise<DiagramDTO> => {
        const response = await apiClient.post('/diagrams', request);
        return unwrap(response);
    },

    /**
     * Update diagram metadata
     */
    updateDiagram: async (
        id: string,
        request: UpdateDiagramRequest
    ): Promise<DiagramDTO> => {
        const response = await apiClient.put(`/diagrams/${id}`, request);
        return unwrap(response);
    },

    /**
     * Delete a diagram
     */
    deleteDiagram: async (id: string): Promise<void> => {
        await apiClient.delete(`/diagrams/${id}`);
    },

    // ===================
    // TABLES
    // ===================

    /**
     * Get all tables for a diagram
     */
    getTables: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/tables`
            );
            const data = unwrap<any[]>(response);
            return (data || []).map(mapTableFromBackend);
        } catch {
            return [];
        }
    },

    /**
     * Create a table
     */
    createTable: async (diagramId: string, table: any): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/tables`,
            mapTableToBackend(table)
        );
        return mapTableFromBackend(unwrap(response));
    },

    /**
     * Update a table
     */
    updateTable: async (
        diagramId: string,
        tableId: string,
        table: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/tables/${tableId}`,
            mapTableToBackend(table)
        );
        return mapTableFromBackend(unwrap(response));
    },

    /**
     * Delete a table
     */
    deleteTable: async (diagramId: string, tableId: string): Promise<void> => {
        await apiClient.delete(`/diagrams/${diagramId}/tables/${tableId}`);
    },

    // ===================
    // COLUMNS
    // ===================

    /**
     * Get columns for a table
     */
    getColumns: async (diagramId: string, tableId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/tables/${tableId}/columns`
            );
            const data = unwrap<any[]>(response);
            return (data || []).map(mapColumnFromBackend);
        } catch {
            return [];
        }
    },

    /**
     * Create a column
     */
    createColumn: async (
        diagramId: string,
        tableId: string,
        column: any
    ): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/tables/${tableId}/columns`,
            mapColumnToBackend(column)
        );
        return mapColumnFromBackend(unwrap(response));
    },

    /**
     * Update a column
     */
    updateColumn: async (
        diagramId: string,
        tableId: string,
        columnId: string,
        column: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/tables/${tableId}/columns/${columnId}`,
            mapColumnToBackend(column)
        );
        return mapColumnFromBackend(unwrap(response));
    },

    /**
     * Delete a column
     */
    deleteColumn: async (
        diagramId: string,
        tableId: string,
        columnId: string
    ): Promise<void> => {
        await apiClient.delete(
            `/diagrams/${diagramId}/tables/${tableId}/columns/${columnId}`
        );
    },

    // ===================
    // RELATIONSHIPS
    // ===================

    /**
     * Get all relationships for a diagram
     */
    getRelationships: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/relationships`
            );
            const data = unwrap<any[]>(response);
            return (data || []).map(mapRelationshipFromBackend);
        } catch {
            return [];
        }
    },

    /**
     * Create a relationship
     */
    createRelationship: async (
        diagramId: string,
        relationship: any
    ): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/relationships`,
            mapRelationshipToBackend(relationship)
        );
        return mapRelationshipFromBackend(unwrap(response));
    },

    /**
     * Update a relationship
     */
    updateRelationship: async (
        diagramId: string,
        relationshipId: string,
        relationship: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/relationships/${relationshipId}`,
            mapRelationshipToBackend(relationship)
        );
        return mapRelationshipFromBackend(unwrap(response));
    },

    /**
     * Delete a relationship
     */
    deleteRelationship: async (
        diagramId: string,
        relationshipId: string
    ): Promise<void> => {
        await apiClient.delete(
            `/diagrams/${diagramId}/relationships/${relationshipId}`
        );
    },

    // ===================
    // DEPENDENCIES
    // ===================

    getDependencies: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/dependencies`
            );
            return unwrap<any[]>(response) || [];
        } catch {
            return [];
        }
    },

    createDependency: async (
        diagramId: string,
        dependency: any
    ): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/dependencies`,
            dependency
        );
        return unwrap(response);
    },

    updateDependency: async (
        diagramId: string,
        dependencyId: string,
        dependency: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/dependencies/${dependencyId}`,
            dependency
        );
        return unwrap(response);
    },

    deleteDependency: async (
        diagramId: string,
        dependencyId: string
    ): Promise<void> => {
        await apiClient.delete(
            `/diagrams/${diagramId}/dependencies/${dependencyId}`
        );
    },

    // ===================
    // AREAS
    // ===================

    getAreas: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/areas`
            );
            const data = unwrap<any[]>(response);
            return (data || []).map(mapAreaFromBackend);
        } catch {
            return [];
        }
    },

    createArea: async (diagramId: string, area: any): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/areas`,
            mapAreaToBackend(area)
        );
        return mapAreaFromBackend(unwrap(response));
    },

    updateArea: async (
        diagramId: string,
        areaId: string,
        area: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/areas/${areaId}`,
            mapAreaToBackend(area)
        );
        return mapAreaFromBackend(unwrap(response));
    },

    deleteArea: async (diagramId: string, areaId: string): Promise<void> => {
        await apiClient.delete(`/diagrams/${diagramId}/areas/${areaId}`);
    },

    // ===================
    // CUSTOM TYPES
    // ===================

    getCustomTypes: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/custom-types`
            );
            return unwrap<any[]>(response) || [];
        } catch {
            return [];
        }
    },

    createCustomType: async (
        diagramId: string,
        customType: any
    ): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/custom-types`,
            customType
        );
        return unwrap(response);
    },

    updateCustomType: async (
        diagramId: string,
        customTypeId: string,
        customType: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/custom-types/${customTypeId}`,
            customType
        );
        return unwrap(response);
    },

    deleteCustomType: async (
        diagramId: string,
        customTypeId: string
    ): Promise<void> => {
        await apiClient.delete(
            `/diagrams/${diagramId}/custom-types/${customTypeId}`
        );
    },

    // ===================
    // NOTES
    // ===================

    getNotes: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/notes`
            );
            const data = unwrap<any[]>(response);
            return (data || []).map(mapNoteFromBackend);
        } catch {
            return [];
        }
    },

    createNote: async (diagramId: string, note: any): Promise<any> => {
        const response = await apiClient.post(
            `/diagrams/${diagramId}/notes`,
            mapNoteToBackend(note)
        );
        return mapNoteFromBackend(unwrap(response));
    },

    updateNote: async (
        diagramId: string,
        noteId: string,
        note: any
    ): Promise<any> => {
        const response = await apiClient.put(
            `/diagrams/${diagramId}/notes/${noteId}`,
            mapNoteToBackend(note)
        );
        return mapNoteFromBackend(unwrap(response));
    },

    deleteNote: async (diagramId: string, noteId: string): Promise<void> => {
        await apiClient.delete(`/diagrams/${diagramId}/notes/${noteId}`);
    },

    // ===================
    // SHARING
    // ===================

    /**
     * Share diagram with a user
     */
    shareDiagram: async (
        diagramId: string,
        email: string,
        permission: 'VIEW' | 'EDIT' | 'ADMIN'
    ): Promise<void> => {
        // Map frontend permission names to backend PermissionLevel enum
        const permissionMap: Record<string, string> = {
            VIEW: 'VIEWER',
            EDIT: 'EDITOR',
            ADMIN: 'ADMIN',
        };
        await apiClient.post(`/diagrams/${diagramId}/share`, {
            email,
            permissionLevel: permissionMap[permission] || 'VIEWER',
        });
    },

    /**
     * Remove sharing for a user
     */
    unshareDiagram: async (
        diagramId: string,
        userId: string
    ): Promise<void> => {
        await apiClient.delete(`/diagrams/${diagramId}/permissions/${userId}`);
    },

    /**
     * Update permission level for a user
     */
    updatePermission: async (
        diagramId: string,
        userId: string,
        permission: 'VIEW' | 'EDIT' | 'ADMIN'
    ): Promise<void> => {
        const permissionMap: Record<string, string> = {
            VIEW: 'VIEWER',
            EDIT: 'EDITOR',
            ADMIN: 'ADMIN',
        };
        await apiClient.put(`/diagrams/${diagramId}/permissions/${userId}`, {
            permissionLevel: permissionMap[permission] || 'VIEWER',
        });
    },

    /**
     * Get list of users with permissions for a diagram
     */
    getCollaborators: async (diagramId: string): Promise<any[]> => {
        try {
            const response = await apiClient.get(
                `/diagrams/${diagramId}/permissions`
            );
            const permissions = unwrap<any[]>(response) || [];
            // Map PermissionResponse to Collaborator format expected by UI
            return permissions.map((p: any) => ({
                userId: p.userId || p.id,
                email: p.userEmail || p.invitedEmail,
                name: p.userDisplayName,
                permission: mapPermissionLevel(p.permissionLevel),
                isOwner: p.permissionLevel === 'OWNER',
                avatarUrl: p.userAvatarUrl,
                invitationStatus: p.invitationStatus,
            }));
        } catch {
            return [];
        }
    },
};

// Map backend PermissionLevel to frontend Permission type
const mapPermissionLevel = (level: string): 'VIEW' | 'EDIT' | 'ADMIN' => {
    switch (level) {
        case 'OWNER':
        case 'ADMIN':
            return 'ADMIN';
        case 'EDITOR':
            return 'EDIT';
        case 'COMMENTER':
        case 'VIEWER':
        default:
            return 'VIEW';
    }
};

// ===================
// MAPPERS
// ===================

// Map frontend table to backend format
const mapTableToBackend = (table: any) => ({
    id: table.id,
    name: table.name,
    schema: table.schema,
    positionX: table.x,
    positionY: table.y,
    color: table.color,
    isView: table.isView,
    isMaterializedView: table.isMaterializedView,
    width: table.width,
    comment: table.comments,
    isCollapsed: table.expanded === false,
    sortOrder: table.order,
    columns: table.fields?.map(mapColumnToBackend) || [],
    // Send indexes as JSON string for backend storage
    indexes: table.indexes ? JSON.stringify(table.indexes) : null,
});

// Helper to parse indexes from backend
const parseIndexes = (indexesJson: string | null | undefined): any[] => {
    if (!indexesJson) return [];
    try {
        return JSON.parse(indexesJson);
    } catch {
        return [];
    }
};

// Map backend table to frontend format
const mapTableFromBackend = (dto: any) => ({
    id: dto.id,
    name: dto.name,
    schema: dto.schemaName,
    x: dto.positionX || 0,
    y: dto.positionY || 0,
    color: dto.color || '#3b82f6',
    isView: dto.isView || false,
    isMaterializedView: dto.isMaterializedView,
    width: dto.width,
    comments: dto.description,
    order: dto.sortOrder,
    expanded: !dto.isCollapsed,
    createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
    fields: dto.columns?.map(mapColumnFromBackend) || [],
    indexes: parseIndexes(dto.indexes),
});

// Map frontend column/field to backend format
const mapColumnToBackend = (field: any) => ({
    id: field.id,
    name: field.name,
    dataType: typeof field.type === 'object' ? field.type.id : field.type,
    isPrimaryKey: field.primaryKey,
    isUnique: field.unique,
    isNullable: field.nullable,
    isAutoIncrement: field.increment,
    isArray: field.isArray,
    maxLength: field.characterMaximumLength,
    precision: field.precision,
    scale: field.scale,
    defaultValue: field.default,
    collation: field.collation,
    comment: field.comments,
    orderIndex: field.order,
});

// Map backend column to frontend field format
const mapColumnFromBackend = (dto: any) => ({
    id: dto.id,
    name: dto.name,
    type: { id: dto.dataType, name: dto.dataType },
    primaryKey: dto.isPrimaryKey || false,
    unique: dto.isUnique || false,
    nullable: dto.isNullable ?? true,
    increment: dto.isAutoIncrement,
    isArray: dto.isArray,
    characterMaximumLength: dto.maxLength,
    precision: dto.precision,
    scale: dto.scale,
    default: dto.defaultValue,
    collation: dto.collation,
    comments: dto.comment,
    order: dto.orderIndex,
    createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
});

// Map frontend relationship to backend format
const mapRelationshipToBackend = (rel: any) => ({
    id: rel.id,
    name: rel.name,
    sourceTableId: rel.sourceTableId,
    targetTableId: rel.targetTableId,
    sourceColumnId: rel.sourceFieldId,
    targetColumnId: rel.targetFieldId,
    sourceCardinality: rel.sourceCardinality?.toUpperCase() || 'ONE',
    targetCardinality: rel.targetCardinality?.toUpperCase() || 'MANY',
});

// Map backend relationship to frontend format
const mapRelationshipFromBackend = (dto: any) => ({
    id: dto.id,
    name: dto.name,
    sourceTableId: dto.sourceTableId,
    targetTableId: dto.targetTableId,
    sourceFieldId: dto.sourceColumnId,
    targetFieldId: dto.targetColumnId,
    sourceCardinality: dto.sourceCardinality?.toLowerCase() || 'one',
    targetCardinality: dto.targetCardinality?.toLowerCase() || 'many',
    createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
});

// Map area to/from backend
const mapAreaToBackend = (area: any) => ({
    id: area.id,
    name: area.name,
    positionX: area.x,
    positionY: area.y,
    width: area.width,
    height: area.height,
    color: area.color,
});

const mapAreaFromBackend = (dto: any) => ({
    id: dto.id,
    name: dto.name,
    x: dto.positionX || 0,
    y: dto.positionY || 0,
    width: dto.width || 200,
    height: dto.height || 200,
    color: dto.color || '#f0f0f0',
});

// Map note to/from backend
const mapNoteToBackend = (note: any) => ({
    id: note.id,
    content: note.content,
    positionX: note.x,
    positionY: note.y,
    width: note.width,
    height: note.height,
    color: note.color,
});

const mapNoteFromBackend = (dto: any) => ({
    id: dto.id,
    content: dto.content,
    x: dto.positionX || 0,
    y: dto.positionY || 0,
    width: dto.width || 200,
    height: dto.height || 100,
    color: dto.color || '#fef08a',
});

export default diagramsApi;
