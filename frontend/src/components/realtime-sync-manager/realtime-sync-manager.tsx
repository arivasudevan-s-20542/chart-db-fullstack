import type React from 'react';
import { useEffect, useRef, useCallback } from 'react';
import { useCollaboration } from '@/context/collaboration-context';
import { useChartDB } from '@/hooks/use-chartdb';
import { useAuth } from '@/context/auth-context';
import {
    wsService,
    type DiagramEvent,
    type DiagramEventType,
} from '@/services/api/websocket.service';
import type { ChartDBEvent } from '@/context/chartdb-context/chartdb-context';

/**
 * Component that handles real-time synchronization of diagram changes via WebSocket.
 * Must be placed inside both ChartDBProvider and CollaborationProvider.
 */
export const RealtimeSyncManager: React.FC = () => {
    const { currentDiagramId, subscribe, isConnected } = useCollaboration();
    const { user } = useAuth();
    const {
        events,
        addTable,
        updateTable,
        updateTablesState,
        removeTable,
        addField,
        updateField,
        removeField,
        addIndex,
        updateIndex,
        removeIndex,
        addRelationship,
        updateRelationship,
        removeRelationship,
        addArea,
        updateArea,
        removeArea,
        addNote,
        updateNote,
        removeNote,
        loadDiagram,
    } = useChartDB();

    // Track if we're applying remote changes to avoid echo
    const isApplyingRemoteChange = useRef(false);

    // Handler for local ChartDB events - forward to WebSocket
    const handleLocalEvent = useCallback(
        (event: ChartDBEvent) => {
            console.log('[RealtimeSync] Local event received:', event.action, {
                isApplyingRemote: isApplyingRemoteChange.current,
                currentDiagramId,
                isConnected,
            });

            // Don't broadcast if we're applying a remote change or not connected
            if (
                isApplyingRemoteChange.current ||
                !currentDiagramId ||
                !isConnected
            ) {
                console.log(
                    '[RealtimeSync] Skipping broadcast - not connected or applying remote'
                );
                return;
            }

            let wsEventType: DiagramEventType | null = null;
            let payload: any = null;

            switch (event.action) {
                case 'add_tables':
                    // Send individual TABLE_CREATED events for each table
                    event.data.tables.forEach((table) => {
                        console.log(
                            '[RealtimeSync] Sending TABLE_CREATED:',
                            table.name
                        );
                        wsService.sendDiagramEvent(
                            currentDiagramId,
                            'TABLE_CREATED',
                            { table }
                        );
                    });
                    return;

                case 'update_table':
                    wsEventType = 'TABLE_UPDATED';
                    payload = {
                        tableId: event.data.id,
                        changes: event.data.table,
                    };
                    break;

                case 'remove_tables':
                    // Send individual TABLE_DELETED events for each table
                    event.data.tableIds.forEach((tableId) => {
                        wsService.sendDiagramEvent(
                            currentDiagramId,
                            'TABLE_DELETED',
                            { tableId }
                        );
                    });
                    return;

                case 'add_field':
                    wsEventType = 'COLUMN_CREATED';
                    payload = {
                        tableId: event.data.tableId,
                        field: event.data.field,
                    };
                    break;

                case 'update_field':
                    wsEventType = 'COLUMN_UPDATED';
                    payload = {
                        tableId: event.data.tableId,
                        fieldId: event.data.fieldId,
                        field: event.data.field,
                    };
                    break;

                case 'remove_field':
                    wsEventType = 'COLUMN_DELETED';
                    payload = {
                        tableId: event.data.tableId,
                        fieldId: event.data.fieldId,
                    };
                    break;

                case 'add_index':
                    wsEventType = 'INDEX_CREATED';
                    payload = {
                        tableId: event.data.tableId,
                        index: event.data.index,
                    };
                    break;

                case 'remove_index':
                    wsEventType = 'INDEX_DELETED';
                    payload = {
                        tableId: event.data.tableId,
                        indexId: event.data.indexId,
                    };
                    break;

                case 'update_index':
                    wsEventType = 'INDEX_UPDATED';
                    payload = {
                        tableId: event.data.tableId,
                        indexId: event.data.indexId,
                        index: event.data.index,
                    };
                    break;

                case 'add_relationship':
                    wsEventType = 'RELATIONSHIP_CREATED';
                    payload = { relationship: event.data.relationship };
                    break;

                case 'remove_relationship':
                    wsEventType = 'RELATIONSHIP_DELETED';
                    payload = { relationshipId: event.data.relationshipId };
                    break;

                case 'update_relationship':
                    wsEventType = 'RELATIONSHIP_UPDATED';
                    payload = {
                        relationshipId: event.data.relationshipId,
                        changes: event.data.relationship, // Use 'changes' to match receiver
                    };
                    break;

                case 'add_area':
                    wsEventType = 'AREA_CREATED';
                    payload = { area: event.data.area };
                    break;

                case 'remove_area':
                    wsEventType = 'AREA_DELETED';
                    payload = { areaId: event.data.areaId };
                    break;

                case 'update_area':
                    wsEventType = 'AREA_UPDATED';
                    payload = {
                        areaId: event.data.areaId,
                        changes: event.data.area,
                    };
                    break;

                case 'add_note':
                    wsEventType = 'NOTE_CREATED';
                    payload = { note: event.data.note };
                    break;

                case 'remove_note':
                    wsEventType = 'NOTE_DELETED';
                    payload = { noteId: event.data.noteId };
                    break;

                case 'update_note':
                    wsEventType = 'NOTE_UPDATED';
                    payload = {
                        noteId: event.data.noteId,
                        changes: event.data.note,
                    };
                    break;

                case 'update_tables_batch':
                    // Batch update for auto-arrange and bulk position changes
                    wsEventType = 'TABLES_BATCH_UPDATED';
                    payload = { tables: event.data.tables };
                    break;

                default:
                    return;
            }

            if (wsEventType && payload) {
                console.log(
                    '[RealtimeSync] Sending event:',
                    wsEventType,
                    payload
                );
                wsService.sendDiagramEvent(
                    currentDiagramId,
                    wsEventType,
                    payload
                );
            }
        },
        [currentDiagramId, isConnected]
    );

    // Subscribe to local ChartDB events using the hook pattern
    events.useSubscription(handleLocalEvent);

    // Apply remote changes from WebSocket
    useEffect(() => {
        if (!currentDiagramId || !isConnected) {
            console.log('[RealtimeSync] Not subscribing to remote events:', {
                currentDiagramId,
                isConnected,
            });
            return;
        }

        console.log(
            '[RealtimeSync] Subscribing to remote events for diagram:',
            currentDiagramId
        );

        const handleEvent = async (event: DiagramEvent) => {
            console.log(
                '[RealtimeSync] Received remote event:',
                event.type,
                event
            );

            // Ignore events from same tab/session to avoid echo
            // If sessionId is null, it came from REST API (e.g., import) - apply to all tabs
            // If sessionId matches ours, ignore (it's from this same tab via WebSocket)
            if (
                event.sessionId &&
                event.sessionId === wsService.getSessionId()
            ) {
                console.log('[RealtimeSync] Ignoring own session event');
                return;
            }

            isApplyingRemoteChange.current = true;

            try {
                switch (event.type) {
                    case 'TABLE_CREATED':
                        console.log('[RealtimeSync] Applying TABLE_CREATED');
                        if (event.payload?.table) {
                            // Ensure fields array exists to prevent crashes
                            const tableWithDefaults = {
                                ...event.payload.table,
                                fields: event.payload.table.fields ?? [],
                                indexes: event.payload.table.indexes ?? [],
                            };
                            await addTable(tableWithDefaults, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'TABLE_UPDATED':
                        console.log(
                            '[RealtimeSync] Applying TABLE_UPDATED:',
                            event.payload
                        );
                        if (event.payload?.tableId && event.payload?.changes) {
                            await updateTable(
                                event.payload.tableId,
                                event.payload.changes,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'TABLES_BATCH_UPDATED':
                        console.log(
                            '[RealtimeSync] Applying TABLES_BATCH_UPDATED:',
                            event.payload?.tables?.length,
                            'tables'
                        );
                        if (
                            event.payload?.tables &&
                            Array.isArray(event.payload.tables)
                        ) {
                            // Apply batch position updates efficiently
                            await updateTablesState(
                                (currentTables) =>
                                    currentTables.map((table) => {
                                        const update =
                                            event.payload.tables.find(
                                                (t: { id: string }) =>
                                                    t.id === table.id
                                            );
                                        if (update) {
                                            return {
                                                ...table,
                                                x: update.x ?? table.x,
                                                y: update.y ?? table.y,
                                                width:
                                                    update.width ?? table.width,
                                            };
                                        }
                                        return table;
                                    }),
                                {
                                    updateHistory: false,
                                    forceOverride: false,
                                }
                            );
                        }
                        break;

                    case 'TABLE_DELETED':
                        if (event.payload?.tableId) {
                            await removeTable(event.payload.tableId, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'COLUMN_CREATED':
                        console.log(
                            '[RealtimeSync] Applying COLUMN_CREATED:',
                            event.payload
                        );
                        if (event.payload?.tableId && event.payload?.field) {
                            // Ensure field has required properties
                            const fieldWithDefaults = {
                                ...event.payload.field,
                                createdAt:
                                    event.payload.field.createdAt ?? Date.now(),
                            };
                            await addField(
                                event.payload.tableId,
                                fieldWithDefaults,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'COLUMN_UPDATED':
                        if (
                            event.payload?.tableId &&
                            event.payload?.fieldId &&
                            event.payload?.field
                        ) {
                            await updateField(
                                event.payload.tableId,
                                event.payload.fieldId,
                                event.payload.field,
                                { updateHistory: false }
                            );
                        }
                        break;

                    case 'COLUMN_DELETED':
                        if (event.payload?.tableId && event.payload?.fieldId) {
                            await removeField(
                                event.payload.tableId,
                                event.payload.fieldId,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'INDEX_CREATED':
                        if (event.payload?.tableId && event.payload?.index) {
                            await addIndex(
                                event.payload.tableId,
                                event.payload.index,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'INDEX_UPDATED':
                        if (
                            event.payload?.tableId &&
                            event.payload?.indexId &&
                            event.payload?.index
                        ) {
                            await updateIndex(
                                event.payload.tableId,
                                event.payload.indexId,
                                event.payload.index,
                                { updateHistory: false }
                            );
                        }
                        break;

                    case 'INDEX_DELETED':
                        if (event.payload?.tableId && event.payload?.indexId) {
                            await removeIndex(
                                event.payload.tableId,
                                event.payload.indexId,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'RELATIONSHIP_CREATED':
                        if (event.payload?.relationship) {
                            await addRelationship(event.payload.relationship, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'RELATIONSHIP_UPDATED':
                        if (
                            event.payload?.relationshipId &&
                            event.payload?.changes
                        ) {
                            await updateRelationship(
                                event.payload.relationshipId,
                                event.payload.changes,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'RELATIONSHIP_DELETED':
                        if (event.payload?.relationshipId) {
                            await removeRelationship(
                                event.payload.relationshipId,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'AREA_CREATED':
                        if (event.payload?.area) {
                            await addArea(event.payload.area, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'AREA_UPDATED':
                        if (event.payload?.areaId && event.payload?.changes) {
                            await updateArea(
                                event.payload.areaId,
                                event.payload.changes,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'AREA_DELETED':
                        if (event.payload?.areaId) {
                            await removeArea(event.payload.areaId, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'NOTE_CREATED':
                        if (event.payload?.note) {
                            await addNote(event.payload.note, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'NOTE_UPDATED':
                        if (event.payload?.noteId && event.payload?.changes) {
                            await updateNote(
                                event.payload.noteId,
                                event.payload.changes,
                                {
                                    updateHistory: false,
                                }
                            );
                        }
                        break;

                    case 'NOTE_DELETED':
                        if (event.payload?.noteId) {
                            await removeNote(event.payload.noteId, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'DIAGRAM_UPDATED':
                        console.log(
                            '[RealtimeSync] Diagram updated by another user'
                        );
                        break;

                    case 'AI_ACTION':
                        // AI made changes - reload the entire diagram to see the changes
                        console.log(
                            '[RealtimeSync] AI action detected, reloading diagram:',
                            event.payload
                        );
                        if (currentDiagramId) {
                            await loadDiagram(currentDiagramId);
                        }
                        break;

                    default:
                        // Cursor, selection, lock events are handled elsewhere
                        break;
                }
            } catch (error) {
                console.error(
                    '[RealtimeSync] Error applying remote change:',
                    error
                );
            } finally {
                isApplyingRemoteChange.current = false;
            }
        };

        // Subscribe to all diagram events
        const unsubscribe = subscribe('*', handleEvent);

        return () => {
            unsubscribe();
        };
    }, [
        currentDiagramId,
        isConnected,
        user?.id,
        subscribe,
        addTable,
        updateTable,
        updateTablesState,
        removeTable,
        addField,
        updateField,
        removeField,
        addIndex,
        updateIndex,
        removeIndex,
        addRelationship,
        updateRelationship,
        removeRelationship,
        addArea,
        updateArea,
        removeArea,
        addNote,
        updateNote,
        removeNote,
        loadDiagram,
    ]);

    // This component doesn't render anything
    return null;
};

export default RealtimeSyncManager;
