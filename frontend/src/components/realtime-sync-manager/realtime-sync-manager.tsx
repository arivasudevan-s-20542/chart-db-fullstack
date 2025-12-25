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
        removeTable,
        addField,
        updateField,
        removeField,
        addRelationship,
        updateRelationship,
        removeRelationship,
        addArea,
        updateArea,
        removeArea,
        addNote,
        updateNote,
        removeNote,
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

                case 'remove_field':
                    wsEventType = 'COLUMN_DELETED';
                    payload = {
                        tableId: event.data.tableId,
                        fieldId: event.data.fieldId,
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
                        relationship: event.data.relationship,
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

            // Ignore own events
            if (event.userId === user?.id) {
                console.log('[RealtimeSync] Ignoring own event');
                return;
            }

            isApplyingRemoteChange.current = true;

            try {
                switch (event.type) {
                    case 'TABLE_CREATED':
                        console.log('[RealtimeSync] Applying TABLE_CREATED');
                        if (event.payload?.table) {
                            await addTable(event.payload.table, {
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

                    case 'TABLE_DELETED':
                        if (event.payload?.tableId) {
                            await removeTable(event.payload.tableId, {
                                updateHistory: false,
                            });
                        }
                        break;

                    case 'COLUMN_CREATED':
                        if (event.payload?.tableId && event.payload?.field) {
                            await addField(
                                event.payload.tableId,
                                event.payload.field,
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
                            event.payload?.changes
                        ) {
                            await updateField(
                                event.payload.tableId,
                                event.payload.fieldId,
                                event.payload.changes,
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
        removeTable,
        addField,
        updateField,
        removeField,
        addRelationship,
        updateRelationship,
        removeRelationship,
        addArea,
        updateArea,
        removeArea,
        addNote,
        updateNote,
        removeNote,
    ]);

    // This component doesn't render anything
    return null;
};

export default RealtimeSyncManager;
