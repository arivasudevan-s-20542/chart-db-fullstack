import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import type {
    DatabaseConnection,
    CreateConnectionRequest,
    TestConnectionRequest,
    QueryRequest,
    QueryResultResponse,
    QueryHistoryItem,
    SavedQuery,
} from '@/types/database.types';
import { ConnectionStatus } from '@/types/database.types';
import { databaseApi } from '@/services/api/database.api';

interface DatabaseConnectionState {
    // Connections
    connections: DatabaseConnection[];
    activeConnectionId: string | null;
    isLoadingConnections: boolean;
    connectionError: string | null;

    // Query execution
    queryResults: QueryResultResponse | null;
    isExecutingQuery: boolean;
    queryError: string | null;

    // Query history
    queryHistory: QueryHistoryItem[];
    isLoadingHistory: boolean;

    // Saved queries
    savedQueries: SavedQuery[];
    isLoadingSavedQueries: boolean;

    // Actions
    loadConnections: (diagramId: string) => Promise<void>;
    createConnection: (
        request: CreateConnectionRequest
    ) => Promise<DatabaseConnection>;
    deleteConnection: (connectionId: string) => Promise<void>;
    testConnection: (request: TestConnectionRequest) => Promise<boolean>;
    testExistingConnection: (connectionId: string) => Promise<boolean>;
    setActiveConnection: (connectionId: string | null) => void;
    executeQuery: (
        connectionId: string,
        request: QueryRequest
    ) => Promise<void>;
    loadQueryHistory: (connectionId: string) => Promise<void>;
    saveQuery: (
        connectionId: string,
        name: string,
        description: string,
        sql: string
    ) => Promise<void>;
    loadSavedQueries: (connectionId: string) => Promise<void>;
    clearQueryResults: () => void;
    clearError: () => void;
}

export const useDatabaseConnectionStore = create<DatabaseConnectionState>()(
    devtools(
        (set) => ({
            // Initial state
            connections: [],
            activeConnectionId: null,
            isLoadingConnections: false,
            connectionError: null,
            queryResults: null,
            isExecutingQuery: false,
            queryError: null,
            queryHistory: [],
            isLoadingHistory: false,
            savedQueries: [],
            isLoadingSavedQueries: false,

            // Load connections for a diagram
            loadConnections: async (diagramId: string) => {
                set({ isLoadingConnections: true, connectionError: null });
                try {
                    const connections =
                        await databaseApi.getConnectionsByDiagram(diagramId);
                    set({ connections, isLoadingConnections: false });
                } catch (error) {
                    set({
                        connectionError:
                            error instanceof Error
                                ? error.message
                                : 'Failed to load connections',
                        isLoadingConnections: false,
                    });
                }
            },

            // Create a new connection
            createConnection: async (request: CreateConnectionRequest) => {
                set({ connectionError: null });
                try {
                    const connection =
                        await databaseApi.createConnection(request);
                    set((state) => ({
                        connections: [...state.connections, connection],
                    }));
                    return connection;
                } catch (error) {
                    const message =
                        error instanceof Error
                            ? error.message
                            : 'Failed to create connection';
                    set({ connectionError: message });
                    throw error;
                }
            },

            // Delete a connection
            deleteConnection: async (connectionId: string) => {
                set({ connectionError: null });
                try {
                    await databaseApi.deleteConnection(connectionId);
                    set((state) => ({
                        connections: state.connections.filter(
                            (c) => c.id !== connectionId
                        ),
                        activeConnectionId:
                            state.activeConnectionId === connectionId
                                ? null
                                : state.activeConnectionId,
                    }));
                } catch (error) {
                    const message =
                        error instanceof Error
                            ? error.message
                            : 'Failed to delete connection';
                    set({ connectionError: message });
                    throw error;
                }
            },

            // Test a new connection
            testConnection: async (request: TestConnectionRequest) => {
                try {
                    const result = await databaseApi.testConnection(request);
                    return result.success;
                } catch {
                    return false;
                }
            },

            // Test an existing connection
            testExistingConnection: async (connectionId: string) => {
                try {
                    const result =
                        await databaseApi.testExistingConnection(connectionId);
                    // Update connection status
                    set((state) => ({
                        connections: state.connections.map((c) =>
                            c.id === connectionId
                                ? {
                                      ...c,
                                      status: result.success
                                          ? ConnectionStatus.ONLINE
                                          : ConnectionStatus.ERROR,
                                      lastTestedAt: new Date().toISOString(),
                                      lastError: result.success
                                          ? undefined
                                          : result.message,
                                  }
                                : c
                        ),
                    }));
                    return result.success;
                } catch {
                    return false;
                }
            },

            // Set active connection
            setActiveConnection: (connectionId: string | null) => {
                set({
                    activeConnectionId: connectionId,
                    queryResults: null,
                    queryError: null,
                });
            },

            // Execute a query
            executeQuery: async (
                connectionId: string,
                request: QueryRequest
            ) => {
                set({ isExecutingQuery: true, queryError: null });
                try {
                    const results = await databaseApi.executeQuery(
                        connectionId,
                        request
                    );
                    set({ queryResults: results, isExecutingQuery: false });
                } catch (error) {
                    set({
                        queryError:
                            error instanceof Error
                                ? error.message
                                : 'Failed to execute query',
                        isExecutingQuery: false,
                    });
                }
            },

            // Load query history
            loadQueryHistory: async (connectionId: string) => {
                set({ isLoadingHistory: true });
                try {
                    const history =
                        await databaseApi.getQueryHistory(connectionId);
                    set({ queryHistory: history, isLoadingHistory: false });
                } catch {
                    set({ isLoadingHistory: false });
                }
            },

            // Save a query
            saveQuery: async (
                connectionId: string,
                name: string,
                description: string,
                sql: string
            ) => {
                const savedQuery = await databaseApi.saveQuery(connectionId, {
                    name,
                    description,
                    sql,
                });
                set((state) => ({
                    savedQueries: [...state.savedQueries, savedQuery],
                }));
            },

            // Load saved queries
            loadSavedQueries: async (connectionId: string) => {
                set({ isLoadingSavedQueries: true });
                try {
                    const queries =
                        await databaseApi.getSavedQueries(connectionId);
                    set({
                        savedQueries: queries,
                        isLoadingSavedQueries: false,
                    });
                } catch {
                    set({ isLoadingSavedQueries: false });
                }
            },

            // Clear query results
            clearQueryResults: () => {
                set({ queryResults: null, queryError: null });
            },

            // Clear errors
            clearError: () => {
                set({ connectionError: null, queryError: null });
            },
        }),
        { name: 'DatabaseConnectionStore' }
    )
);
