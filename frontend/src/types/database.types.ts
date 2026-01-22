/**
 * Database Connection Types
 */

export enum DatabaseType {
    POSTGRESQL = 'postgresql',
    MYSQL = 'mysql',
    SQL_SERVER = 'sqlserver',
    ORACLE = 'oracle',
}

export enum ConnectionStatus {
    ONLINE = 'ONLINE',
    OFFLINE = 'OFFLINE',
    ERROR = 'ERROR',
    TESTING = 'TESTING',
}

export interface DatabaseConnection {
    id: string;
    diagramId: string;
    databaseType: DatabaseType;
    name: string;
    host: string;
    port: number;
    databaseName: string;
    username: string;
    environment?: string;
    status: ConnectionStatus;
    lastTestedAt?: string;
    lastError?: string;
    createdAt: string;
    updatedAt: string;
}

export interface CreateConnectionRequest {
    diagramId: string;
    databaseType: DatabaseType;
    name: string;
    host: string;
    port: number;
    databaseName: string;
    username: string;
    password: string;
    environment?: string;
}

export interface TestConnectionRequest {
    databaseType: DatabaseType;
    host: string;
    port: number;
    databaseName: string;
    username: string;
    password: string;
}

export interface TestConnectionResponse {
    success: boolean;
    message: string;
    connectionTime?: number;
}

export interface QueryRequest {
    sql: string;
    maxRows?: number;
}

export interface QueryResultColumn {
    name: string;
    type: string;
}

export interface QueryResultResponse {
    columns: QueryResultColumn[];
    rows: Record<string, any>[];
    rowCount: number;
    executionTime: number;
    truncated: boolean;
}

export interface QueryHistoryItem {
    id: string;
    sql: string;
    executionTime: number;
    rowCount: number;
    status: 'SUCCESS' | 'ERROR';
    errorMessage?: string;
    executedAt: string;
}

export interface SavedQuery {
    id: string;
    name: string;
    description?: string;
    sql: string;
    createdAt: string;
    updatedAt: string;
}
