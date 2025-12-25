import React, { useCallback } from 'react';
import type { StorageContext } from './storage-context';
import { storageContext, storageInitialValue } from './storage-context';
import type { Diagram } from '@/lib/domain/diagram';
import type { DBTable } from '@/lib/domain/db-table';
import type { DBRelationship } from '@/lib/domain/db-relationship';
import type { ChartDBConfig } from '@/lib/domain/config';
import type { DBDependency } from '@/lib/domain/db-dependency';
import type { Area } from '@/lib/domain/area';
import type { DBCustomType } from '@/lib/domain/db-custom-type';
import type { DiagramFilter } from '@/lib/domain/diagram-filter/diagram-filter';
import type { Note } from '@/lib/domain/note';
import { diagramsApi } from '@/services/api/diagrams.api';
import { isAuthenticated } from '@/services/api/token-storage';
import { DatabaseType } from '@/lib/domain/database-type';

// In-memory cache for diagram data to reduce API calls
interface DiagramCache {
    [diagramId: string]: {
        tables: Map<string, DBTable>;
        relationships: Map<string, DBRelationship>;
        dependencies: Map<string, DBDependency>;
        areas: Map<string, Area>;
        customTypes: Map<string, DBCustomType>;
        notes: Map<string, Note>;
    };
}

// Local storage fallback for config (user preferences)
const CONFIG_KEY = 'chartdb_config';
const FILTER_KEY_PREFIX = 'chartdb_filter_';

export const BackendStorageProvider: React.FC<React.PropsWithChildren> = ({
    children,
}) => {
    // In-memory cache
    const cache: DiagramCache = {};

    const ensureCache = (diagramId: string) => {
        if (!cache[diagramId]) {
            cache[diagramId] = {
                tables: new Map(),
                relationships: new Map(),
                dependencies: new Map(),
                areas: new Map(),
                customTypes: new Map(),
                notes: new Map(),
            };
        }
        return cache[diagramId];
    };

    // Config operations - stored locally
    const getConfig: StorageContext['getConfig'] = useCallback(async () => {
        try {
            const stored = localStorage.getItem(CONFIG_KEY);
            return stored ? JSON.parse(stored) : undefined;
        } catch {
            return undefined;
        }
    }, []);

    const updateConfig: StorageContext['updateConfig'] = useCallback(async (config) => {
        try {
            const current = localStorage.getItem(CONFIG_KEY);
            const merged = { ...(current ? JSON.parse(current) : {}), ...config };
            localStorage.setItem(CONFIG_KEY, JSON.stringify(merged));
        } catch (e) {
            console.error('Failed to update config:', e);
        }
    }, []);

    // Diagram filter operations - stored locally
    const getDiagramFilter: StorageContext['getDiagramFilter'] = useCallback(
        async (diagramId: string) => {
            try {
                const stored = localStorage.getItem(FILTER_KEY_PREFIX + diagramId);
                return stored ? JSON.parse(stored) : undefined;
            } catch {
                return undefined;
            }
        },
        []
    );

    const updateDiagramFilter: StorageContext['updateDiagramFilter'] = useCallback(
        async (diagramId, filter) => {
            localStorage.setItem(FILTER_KEY_PREFIX + diagramId, JSON.stringify(filter));
        },
        []
    );

    const deleteDiagramFilter: StorageContext['deleteDiagramFilter'] = useCallback(
        async (diagramId: string) => {
            localStorage.removeItem(FILTER_KEY_PREFIX + diagramId);
        },
        []
    );

    // Diagram operations
    const addDiagram: StorageContext['addDiagram'] = useCallback(
        async ({ diagram }) => {
            if (!isAuthenticated()) return;

            console.log('[BackendStorageProvider] Creating diagram in backend:', diagram.id);
            await diagramsApi.createDiagram({
                id: diagram.id,  // Pass the frontend-generated ID to backend
                name: diagram.name,
                databaseType: diagram.databaseType,
                databaseEdition: diagram.databaseEdition,
            });
            console.log('[BackendStorageProvider] Diagram created successfully:', diagram.id);

            // Also save tables, relationships, and other entities if present
            const diagramId = diagram.id;

            // Save tables with their fields as columns
            if (diagram.tables && diagram.tables.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.tables.length, 'tables');
                for (const table of diagram.tables) {
                    try {
                        // Map fields to columns for the backend
                        const columns = table.fields?.map((field, index) => ({
                            id: field.id,
                            name: field.name,
                            type: field.type?.name || field.type,
                            isPrimaryKey: field.primaryKey || false,
                            isNullable: field.nullable !== false,
                            isUnique: field.unique || false,
                            defaultValue: field.default,
                            comment: field.comments,
                            orderIndex: index,
                        })) || [];

                        await diagramsApi.createTable(diagramId, {
                            id: table.id,
                            name: table.name,
                            schema: table.schema,
                            positionX: table.x || 0,
                            positionY: table.y || 0,
                            width: table.width,
                            color: table.color,
                            isView: table.isView,
                            columns,
                        });
                        console.log('[BackendStorageProvider] Table saved:', table.name);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save table:', table.name, e);
                    }
                }
            }

            // Save relationships
            if (diagram.relationships && diagram.relationships.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.relationships.length, 'relationships');
                for (const rel of diagram.relationships) {
                    try {
                        await diagramsApi.createRelationship(diagramId, {
                            id: rel.id,
                            name: rel.name,
                            sourceTableId: rel.sourceTableId,
                            targetTableId: rel.targetTableId,
                            sourceFieldId: rel.sourceFieldId,
                            targetFieldId: rel.targetFieldId,
                            sourceCardinality: rel.sourceCardinality,
                            targetCardinality: rel.targetCardinality,
                        });
                        console.log('[BackendStorageProvider] Relationship saved:', rel.name || rel.id);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save relationship:', rel.id, e);
                    }
                }
            }

            // Save areas
            if (diagram.areas && diagram.areas.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.areas.length, 'areas');
                for (const area of diagram.areas) {
                    try {
                        await diagramsApi.createArea(diagramId, area);
                        console.log('[BackendStorageProvider] Area saved:', area.name);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save area:', area.name, e);
                    }
                }
            }

            // Save custom types
            if (diagram.customTypes && diagram.customTypes.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.customTypes.length, 'custom types');
                for (const customType of diagram.customTypes) {
                    try {
                        await diagramsApi.createCustomType(diagramId, customType);
                        console.log('[BackendStorageProvider] Custom type saved:', customType.name);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save custom type:', customType.name, e);
                    }
                }
            }

            // Save notes
            if (diagram.notes && diagram.notes.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.notes.length, 'notes');
                for (const note of diagram.notes) {
                    try {
                        await diagramsApi.createNote(diagramId, note);
                        console.log('[BackendStorageProvider] Note saved:', note.id);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save note:', note.id, e);
                    }
                }
            }

            // Save dependencies
            if (diagram.dependencies && diagram.dependencies.length > 0) {
                console.log('[BackendStorageProvider] Saving', diagram.dependencies.length, 'dependencies');
                for (const dep of diagram.dependencies) {
                    try {
                        await diagramsApi.createDependency(diagramId, dep);
                        console.log('[BackendStorageProvider] Dependency saved:', dep.id);
                    } catch (e) {
                        console.error('[BackendStorageProvider] Failed to save dependency:', dep.id, e);
                    }
                }
            }
        },
        []
    );

    const listDiagrams: StorageContext['listDiagrams'] = useCallback(
        async (options = {}) => {
            if (!isAuthenticated()) return [];

            try {
                const response = await diagramsApi.listDiagrams();
                const diagrams: Diagram[] = response.content.map((dto) => ({
                    id: dto.id,
                    name: dto.name,
                    databaseType: dto.databaseType as DatabaseType,
                    databaseEdition: dto.databaseEdition as any,
                    createdAt: new Date(dto.createdAt),
                    updatedAt: new Date(dto.updatedAt),
                    tables: [],
                    relationships: [],
                    dependencies: [],
                    areas: [],
                    customTypes: [],
                    notes: [],
                }));
                return diagrams;
            } catch (e) {
                console.error('Failed to list diagrams:', e);
                return [];
            }
        },
        []
    );

    const getDiagram: StorageContext['getDiagram'] = useCallback(
        async (id: string, options = {}) => {
            console.log('[BackendStorageProvider] getDiagram called with id:', id);
            console.log('[BackendStorageProvider] isAuthenticated:', isAuthenticated());
            
            if (!isAuthenticated()) {
                console.log('[BackendStorageProvider] Not authenticated, returning undefined');
                return undefined;
            }

            try {
                console.log('[BackendStorageProvider] Fetching diagram from API...');
                const diagram = await diagramsApi.getDiagram(id);
                console.log('[BackendStorageProvider] Diagram fetched:', diagram);

                // Update cache
                const diagramCache = ensureCache(id);
                diagram.tables?.forEach((t) => diagramCache.tables.set(t.id, t));
                diagram.relationships?.forEach((r) => diagramCache.relationships.set(r.id, r));
                diagram.dependencies?.forEach((d) => diagramCache.dependencies.set(d.id, d));
                diagram.areas?.forEach((a) => diagramCache.areas.set(a.id, a));
                diagram.customTypes?.forEach((c) => diagramCache.customTypes.set(c.id, c));
                diagram.notes?.forEach((n) => diagramCache.notes.set(n.id, n));

                return diagram;
            } catch (e) {
                console.error('[BackendStorageProvider] Failed to get diagram:', e);
                return undefined;
            }
        },
        []
    );

    const updateDiagram: StorageContext['updateDiagram'] = useCallback(
        async ({ id, attributes }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.updateDiagram(id, {
                    name: attributes.name,
                    databaseType: attributes.databaseType,
                    databaseEdition: attributes.databaseEdition,
                });
            } catch (e) {
                console.error('Failed to update diagram:', e);
            }
        },
        []
    );

    const deleteDiagram: StorageContext['deleteDiagram'] = useCallback(async (id: string) => {
        if (!isAuthenticated()) return;

        try {
            await diagramsApi.deleteDiagram(id);
            delete cache[id];
        } catch (e) {
            console.error('Failed to delete diagram:', e);
        }
    }, []);

    // Table operations
    const addTable: StorageContext['addTable'] = useCallback(
        async ({ diagramId, table }) => {
            console.log('[BackendStorageProvider] addTable called with diagramId:', diagramId, 'table:', table.name);
            if (!diagramId) {
                console.error('[BackendStorageProvider] ERROR: addTable called with empty diagramId!');
                return;
            }
            if (!isAuthenticated()) {
                console.log('[BackendStorageProvider] Not authenticated, skipping addTable');
                return;
            }

            try {
                const created = await diagramsApi.createTable(diagramId, table);
                console.log('[BackendStorageProvider] Table created successfully:', created.id);
                const diagramCache = ensureCache(diagramId);
                diagramCache.tables.set(created.id, created);
            } catch (e) {
                console.error('[BackendStorageProvider] Failed to add table:', e);
            }
        },
        []
    );

    const getTable: StorageContext['getTable'] = useCallback(
        async ({ diagramId, id }) => {
            const diagramCache = cache[diagramId];
            return diagramCache?.tables.get(id);
        },
        []
    );

    const updateTable: StorageContext['updateTable'] = useCallback(
        async ({ id, attributes }) => {
            if (!isAuthenticated()) return;

            // Find diagram ID from cache
            for (const [diagramId, diagramCache] of Object.entries(cache)) {
                if (diagramCache.tables.has(id)) {
                    try {
                        const table = diagramCache.tables.get(id)!;
                        const updated = await diagramsApi.updateTable(diagramId, id, {
                            ...table,
                            ...attributes,
                        });
                        diagramCache.tables.set(id, updated);
                    } catch (e) {
                        console.error('Failed to update table:', e);
                    }
                    break;
                }
            }
        },
        []
    );

    const putTable: StorageContext['putTable'] = useCallback(
        async ({ diagramId, table }) => {
            if (!isAuthenticated()) return;

            try {
                const diagramCache = ensureCache(diagramId);
                if (diagramCache.tables.has(table.id)) {
                    const updated = await diagramsApi.updateTable(diagramId, table.id, table);
                    diagramCache.tables.set(table.id, updated);
                } else {
                    const created = await diagramsApi.createTable(diagramId, table);
                    diagramCache.tables.set(created.id, created);
                }
            } catch (e) {
                console.error('Failed to put table:', e);
            }
        },
        []
    );

    const deleteTable: StorageContext['deleteTable'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteTable(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.tables.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete table:', e);
            }
        },
        []
    );

    const listTables: StorageContext['listTables'] = useCallback(async (diagramId: string) => {
        if (!isAuthenticated()) return [];

        try {
            const tables = await diagramsApi.getTables(diagramId);
            const diagramCache = ensureCache(diagramId);
            tables.forEach((t) => diagramCache.tables.set(t.id, t));
            return tables;
        } catch (e) {
            console.error('Failed to list tables:', e);
            return [];
        }
    }, []);

    const deleteDiagramTables: StorageContext['deleteDiagramTables'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                // Delete each table via API
                for (const id of diagramCache.tables.keys()) {
                    try {
                        await diagramsApi.deleteTable(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete table:', e);
                    }
                }
                diagramCache.tables.clear();
            }
        },
        []
    );

    // Relationship operations
    const addRelationship: StorageContext['addRelationship'] = useCallback(
        async ({ diagramId, relationship }) => {
            if (!isAuthenticated()) return;

            try {
                const created = await diagramsApi.createRelationship(diagramId, relationship);
                const diagramCache = ensureCache(diagramId);
                diagramCache.relationships.set(created.id, created);
            } catch (e) {
                console.error('Failed to add relationship:', e);
            }
        },
        []
    );

    const getRelationship: StorageContext['getRelationship'] = useCallback(
        async ({ diagramId, id }) => {
            const diagramCache = cache[diagramId];
            return diagramCache?.relationships.get(id);
        },
        []
    );

    const updateRelationship: StorageContext['updateRelationship'] = useCallback(
        async ({ id, attributes }) => {
            if (!isAuthenticated()) return;

            for (const [diagramId, diagramCache] of Object.entries(cache)) {
                if (diagramCache.relationships.has(id)) {
                    try {
                        const relationship = diagramCache.relationships.get(id)!;
                        const updated = await diagramsApi.updateRelationship(diagramId, id, {
                            ...relationship,
                            ...attributes,
                        });
                        diagramCache.relationships.set(id, updated);
                    } catch (e) {
                        console.error('Failed to update relationship:', e);
                    }
                    break;
                }
            }
        },
        []
    );

    const deleteRelationship: StorageContext['deleteRelationship'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteRelationship(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.relationships.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete relationship:', e);
            }
        },
        []
    );

    const listRelationships: StorageContext['listRelationships'] = useCallback(
        async (diagramId: string) => {
            if (!isAuthenticated()) return [];

            try {
                const relationships = await diagramsApi.getRelationships(diagramId);
                const diagramCache = ensureCache(diagramId);
                relationships.forEach((r) => diagramCache.relationships.set(r.id, r));
                return relationships;
            } catch (e) {
                console.error('Failed to list relationships:', e);
                return [];
            }
        },
        []
    );

    const deleteDiagramRelationships: StorageContext['deleteDiagramRelationships'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                for (const id of diagramCache.relationships.keys()) {
                    try {
                        await diagramsApi.deleteRelationship(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete relationship:', e);
                    }
                }
                diagramCache.relationships.clear();
            }
        },
        []
    );

    // Dependency operations
    const addDependency: StorageContext['addDependency'] = useCallback(
        async ({ diagramId, dependency }) => {
            if (!isAuthenticated()) return;

            try {
                const created = await diagramsApi.createDependency(diagramId, dependency);
                const diagramCache = ensureCache(diagramId);
                diagramCache.dependencies.set(created.id, created);
            } catch (e) {
                console.error('Failed to add dependency:', e);
            }
        },
        []
    );

    const getDependency: StorageContext['getDependency'] = useCallback(
        async ({ diagramId, id }) => {
            const diagramCache = cache[diagramId];
            return diagramCache?.dependencies.get(id);
        },
        []
    );

    const updateDependency: StorageContext['updateDependency'] = useCallback(
        async ({ id, attributes }) => {
            if (!isAuthenticated()) return;

            for (const [diagramId, diagramCache] of Object.entries(cache)) {
                if (diagramCache.dependencies.has(id)) {
                    try {
                        const dependency = diagramCache.dependencies.get(id)!;
                        const updated = await diagramsApi.updateDependency(diagramId, id, {
                            ...dependency,
                            ...attributes,
                        });
                        diagramCache.dependencies.set(id, updated);
                    } catch (e) {
                        console.error('Failed to update dependency:', e);
                    }
                    break;
                }
            }
        },
        []
    );

    const deleteDependency: StorageContext['deleteDependency'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteDependency(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.dependencies.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete dependency:', e);
            }
        },
        []
    );

    const listDependencies: StorageContext['listDependencies'] = useCallback(
        async (diagramId: string) => {
            if (!isAuthenticated()) return [];

            try {
                const dependencies = await diagramsApi.getDependencies(diagramId);
                const diagramCache = ensureCache(diagramId);
                dependencies.forEach((d) => diagramCache.dependencies.set(d.id, d));
                return dependencies;
            } catch (e) {
                console.error('Failed to list dependencies:', e);
                return [];
            }
        },
        []
    );

    const deleteDiagramDependencies: StorageContext['deleteDiagramDependencies'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                for (const id of diagramCache.dependencies.keys()) {
                    try {
                        await diagramsApi.deleteDependency(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete dependency:', e);
                    }
                }
                diagramCache.dependencies.clear();
            }
        },
        []
    );

    // Area operations
    const addArea: StorageContext['addArea'] = useCallback(
        async ({ diagramId, area }) => {
            if (!isAuthenticated()) return;

            try {
                const created = await diagramsApi.createArea(diagramId, area);
                const diagramCache = ensureCache(diagramId);
                diagramCache.areas.set(created.id, created);
            } catch (e) {
                console.error('Failed to add area:', e);
            }
        },
        []
    );

    const getArea: StorageContext['getArea'] = useCallback(async ({ diagramId, id }) => {
        const diagramCache = cache[diagramId];
        return diagramCache?.areas.get(id);
    }, []);

    const updateArea: StorageContext['updateArea'] = useCallback(async ({ id, attributes }) => {
        if (!isAuthenticated()) return;

        for (const [diagramId, diagramCache] of Object.entries(cache)) {
            if (diagramCache.areas.has(id)) {
                try {
                    const area = diagramCache.areas.get(id)!;
                    const updated = await diagramsApi.updateArea(diagramId, id, {
                        ...area,
                        ...attributes,
                    });
                    diagramCache.areas.set(id, updated);
                } catch (e) {
                    console.error('Failed to update area:', e);
                }
                break;
            }
        }
    }, []);

    const deleteArea: StorageContext['deleteArea'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteArea(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.areas.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete area:', e);
            }
        },
        []
    );

    const listAreas: StorageContext['listAreas'] = useCallback(async (diagramId: string) => {
        if (!isAuthenticated()) return [];

        try {
            const areas = await diagramsApi.getAreas(diagramId);
            const diagramCache = ensureCache(diagramId);
            areas.forEach((a) => diagramCache.areas.set(a.id, a));
            return areas;
        } catch (e) {
            console.error('Failed to list areas:', e);
            return [];
        }
    }, []);

    const deleteDiagramAreas: StorageContext['deleteDiagramAreas'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                for (const id of diagramCache.areas.keys()) {
                    try {
                        await diagramsApi.deleteArea(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete area:', e);
                    }
                }
                diagramCache.areas.clear();
            }
        },
        []
    );

    // Custom type operations
    const addCustomType: StorageContext['addCustomType'] = useCallback(
        async ({ diagramId, customType }) => {
            if (!isAuthenticated()) return;

            try {
                const created = await diagramsApi.createCustomType(diagramId, customType);
                const diagramCache = ensureCache(diagramId);
                diagramCache.customTypes.set(created.id, created);
            } catch (e) {
                console.error('Failed to add custom type:', e);
            }
        },
        []
    );

    const getCustomType: StorageContext['getCustomType'] = useCallback(
        async ({ diagramId, id }) => {
            const diagramCache = cache[diagramId];
            return diagramCache?.customTypes.get(id);
        },
        []
    );

    const updateCustomType: StorageContext['updateCustomType'] = useCallback(
        async ({ id, attributes }) => {
            if (!isAuthenticated()) return;

            for (const [diagramId, diagramCache] of Object.entries(cache)) {
                if (diagramCache.customTypes.has(id)) {
                    try {
                        const customType = diagramCache.customTypes.get(id)!;
                        const updated = await diagramsApi.updateCustomType(diagramId, id, {
                            ...customType,
                            ...attributes,
                        });
                        diagramCache.customTypes.set(id, updated);
                    } catch (e) {
                        console.error('Failed to update custom type:', e);
                    }
                    break;
                }
            }
        },
        []
    );

    const deleteCustomType: StorageContext['deleteCustomType'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteCustomType(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.customTypes.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete custom type:', e);
            }
        },
        []
    );

    const listCustomTypes: StorageContext['listCustomTypes'] = useCallback(
        async (diagramId: string) => {
            if (!isAuthenticated()) return [];

            try {
                const customTypes = await diagramsApi.getCustomTypes(diagramId);
                const diagramCache = ensureCache(diagramId);
                customTypes.forEach((c) => diagramCache.customTypes.set(c.id, c));
                return customTypes;
            } catch (e) {
                console.error('Failed to list custom types:', e);
                return [];
            }
        },
        []
    );

    const deleteDiagramCustomTypes: StorageContext['deleteDiagramCustomTypes'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                for (const id of diagramCache.customTypes.keys()) {
                    try {
                        await diagramsApi.deleteCustomType(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete custom type:', e);
                    }
                }
                diagramCache.customTypes.clear();
            }
        },
        []
    );

    // Note operations
    const addNote: StorageContext['addNote'] = useCallback(
        async ({ diagramId, note }) => {
            if (!isAuthenticated()) return;

            try {
                const created = await diagramsApi.createNote(diagramId, note);
                const diagramCache = ensureCache(diagramId);
                diagramCache.notes.set(created.id, created);
            } catch (e) {
                console.error('Failed to add note:', e);
            }
        },
        []
    );

    const getNote: StorageContext['getNote'] = useCallback(async ({ diagramId, id }) => {
        const diagramCache = cache[diagramId];
        return diagramCache?.notes.get(id);
    }, []);

    const updateNote: StorageContext['updateNote'] = useCallback(async ({ id, attributes }) => {
        if (!isAuthenticated()) return;

        for (const [diagramId, diagramCache] of Object.entries(cache)) {
            if (diagramCache.notes.has(id)) {
                try {
                    const note = diagramCache.notes.get(id)!;
                    const updated = await diagramsApi.updateNote(diagramId, id, {
                        ...note,
                        ...attributes,
                    });
                    diagramCache.notes.set(id, updated);
                } catch (e) {
                    console.error('Failed to update note:', e);
                }
                break;
            }
        }
    }, []);

    const deleteNote: StorageContext['deleteNote'] = useCallback(
        async ({ diagramId, id }) => {
            if (!isAuthenticated()) return;

            try {
                await diagramsApi.deleteNote(diagramId, id);
                const diagramCache = cache[diagramId];
                if (diagramCache) {
                    diagramCache.notes.delete(id);
                }
            } catch (e) {
                console.error('Failed to delete note:', e);
            }
        },
        []
    );

    const listNotes: StorageContext['listNotes'] = useCallback(async (diagramId: string) => {
        if (!isAuthenticated()) return [];

        try {
            const notes = await diagramsApi.getNotes(diagramId);
            const diagramCache = ensureCache(diagramId);
            notes.forEach((n) => diagramCache.notes.set(n.id, n));
            return notes;
        } catch (e) {
            console.error('Failed to list notes:', e);
            return [];
        }
    }, []);

    const deleteDiagramNotes: StorageContext['deleteDiagramNotes'] = useCallback(
        async (diagramId: string) => {
            const diagramCache = cache[diagramId];
            if (diagramCache) {
                for (const id of diagramCache.notes.keys()) {
                    try {
                        await diagramsApi.deleteNote(diagramId, id);
                    } catch (e) {
                        console.error('Failed to delete note:', e);
                    }
                }
                diagramCache.notes.clear();
            }
        },
        []
    );

    const value: StorageContext = {
        getConfig,
        updateConfig,
        getDiagramFilter,
        updateDiagramFilter,
        deleteDiagramFilter,
        addDiagram,
        listDiagrams,
        getDiagram,
        updateDiagram,
        deleteDiagram,
        addTable,
        getTable,
        updateTable,
        putTable,
        deleteTable,
        listTables,
        deleteDiagramTables,
        addRelationship,
        getRelationship,
        updateRelationship,
        deleteRelationship,
        listRelationships,
        deleteDiagramRelationships,
        addDependency,
        getDependency,
        updateDependency,
        deleteDependency,
        listDependencies,
        deleteDiagramDependencies,
        addArea,
        getArea,
        updateArea,
        deleteArea,
        listAreas,
        deleteDiagramAreas,
        addCustomType,
        getCustomType,
        updateCustomType,
        deleteCustomType,
        listCustomTypes,
        deleteDiagramCustomTypes,
        addNote,
        getNote,
        updateNote,
        deleteNote,
        listNotes,
        deleteDiagramNotes,
    };

    return <storageContext.Provider value={value}>{children}</storageContext.Provider>;
};

export default BackendStorageProvider;
