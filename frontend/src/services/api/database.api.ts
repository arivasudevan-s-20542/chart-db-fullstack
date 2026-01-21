import apiClient from './api-client';
import type {
    DatabaseConnection,
    CreateConnectionRequest,
    TestConnectionRequest,
    TestConnectionResponse,
    QueryRequest,
    QueryResultResponse,
    QueryHistoryItem,
    SavedQuery,
} from '@/types/database.types';

export type { DatabaseConnection, CreateConnectionRequest, TestConnectionRequest, TestConnectionResponse };

export interface QueryExecutionResult {
    success: boolean;
    message: string;
    columns?: Array<{ name: string; type: string }>;
    rows?: Array<Array<any>>;
    rowsAffected?: number;
    executionTimeMs: number;
    errorCode?: string;
}

export interface ExecuteQueryRequest {
    query: string;
    maxRows?: number;
    timeoutSeconds?: number;
}

export const databaseApi = {
    // Connection Management
    createConnection: async (request: CreateConnectionRequest): Promise<DatabaseConnection> => {
        const response = await apiClient.post('/connections', request);
        return response.data.data;
    },

    getConnectionsByDiagram: async (diagramId: string): Promise<DatabaseConnection[]> => {
        const response = await apiClient.get(`/connections/diagram/${diagramId}`);
        return response.data.data;
    },

    getConnection: async (connectionId: string): Promise<DatabaseConnection> => {
        const response = await apiClient.get(`/connections/${connectionId}`);
        return response.data.data;
    },

    deleteConnection: async (connectionId: string): Promise<void> => {
        await apiClient.delete(`/connections/${connectionId}`);
    },

    testConnection: async (request: TestConnectionRequest): Promise<TestConnectionResponse> => {
        const response = await apiClient.post('/connections/test', request);
        return response.data.data;
    },

    testExistingConnection: async (connectionId: string): Promise<TestConnectionResponse> => {
        const response = await apiClient.post(`/connections/${connectionId}/test`);
        return response.data.data;
    },

    // Query Execution
    executeQuery: async (connectionId: string, request: QueryRequest): Promise<QueryResultResponse> => {
        const response = await apiClient.post(`/queries/execute/${connectionId}`, { sql: request.sql, maxRows: request.maxRows });
        return response.data.data;
    },

    getQueryHistory: async (connectionId: string): Promise<QueryHistoryItem[]> => {
        const response = await apiClient.get(`/queries/history`, {
            params: { connectionId },
        });
        return response.data.data;
    },

    saveQuery: async (connectionId: string, request: { name: string; description?: string; sql: string }): Promise<SavedQuery> => {
        const response = await apiClient.post('/queries/saved', { ...request, connectionId });
        return response.data.data;
    },

    getSavedQueries: async (connectionId: string): Promise<SavedQuery[]> => {
        const response = await apiClient.get('/queries/saved', {
            params: { connectionId },
        });
        return response.data.data;
    },
};
