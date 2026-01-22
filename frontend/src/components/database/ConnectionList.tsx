import React, { useEffect } from 'react';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import { Button } from '@/components/button/button';
import { Badge } from '@/components/badge/badge';
import { useDatabaseConnectionStore } from '@/stores/database-connection.store';
import type { DatabaseConnection } from '@/types/database.types';
import { ConnectionStatus, DatabaseType } from '@/types/database.types';
import { Database, Trash2, RefreshCw, Circle } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

interface ConnectionListProps {
    diagramId: string;
    onAddConnection: () => void;
    onSelectConnection: (connection: DatabaseConnection) => void;
}

const getStatusColor = (status: ConnectionStatus) => {
    switch (status) {
        case ConnectionStatus.ONLINE:
            return 'text-green-500';
        case ConnectionStatus.OFFLINE:
            return 'text-gray-400';
        case ConnectionStatus.ERROR:
            return 'text-red-500';
        case ConnectionStatus.TESTING:
            return 'text-yellow-500';
        default:
            return 'text-gray-400';
    }
};

const getStatusLabel = (status: ConnectionStatus) => {
    switch (status) {
        case ConnectionStatus.ONLINE:
            return 'Online';
        case ConnectionStatus.OFFLINE:
            return 'Offline';
        case ConnectionStatus.ERROR:
            return 'Error';
        case ConnectionStatus.TESTING:
            return 'Testing...';
        default:
            return 'Unknown';
    }
};

const getDatabaseTypeLabel = (type: DatabaseType) => {
    switch (type) {
        case DatabaseType.POSTGRESQL:
            return 'PostgreSQL';
        case DatabaseType.MYSQL:
            return 'MySQL';
        case DatabaseType.SQL_SERVER:
            return 'SQL Server';
        case DatabaseType.ORACLE:
            return 'Oracle';
        default:
            return type;
    }
};

export const ConnectionList: React.FC<ConnectionListProps> = ({
    diagramId,
    onAddConnection,
    onSelectConnection,
}) => {
    const {
        connections,
        activeConnectionId,
        isLoadingConnections,
        loadConnections,
        deleteConnection,
        testExistingConnection,
    } = useDatabaseConnectionStore();

    useEffect(() => {
        loadConnections(diagramId);
    }, [diagramId, loadConnections]);

    const handleRefresh = async (connectionId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        await testExistingConnection(connectionId);
    };

    const handleDelete = async (connectionId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (confirm('Are you sure you want to delete this connection?')) {
            await deleteConnection(connectionId);
        }
    };

    if (isLoadingConnections) {
        return (
            <Card>
                <CardHeader>
                    <CardTitle>Database Connections</CardTitle>
                    <CardDescription>Loading connections...</CardDescription>
                </CardHeader>
            </Card>
        );
    }

    return (
        <Card>
            <CardHeader>
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle>Database Connections</CardTitle>
                        <CardDescription>
                            Manage live database connections for this diagram
                        </CardDescription>
                    </div>
                    <Button onClick={onAddConnection} size="sm">
                        <Database className="mr-2 size-4" />
                        Add Connection
                    </Button>
                </div>
            </CardHeader>
            <CardContent>
                {connections.length === 0 ? (
                    <div className="py-8 text-center">
                        <Database className="mx-auto size-12 text-gray-400" />
                        <p className="mt-4 text-sm text-gray-500">
                            No database connections yet
                        </p>
                        <Button
                            onClick={onAddConnection}
                            variant="outline"
                            size="sm"
                            className="mt-4"
                        >
                            Add Your First Connection
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {connections.map((connection) => (
                            <div
                                key={connection.id}
                                onClick={() => onSelectConnection(connection)}
                                className={`
                                    flex cursor-pointer items-center justify-between rounded-lg border-2 p-4 transition-colors
                                    ${
                                        activeConnectionId === connection.id
                                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-950'
                                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                                    }
                                `}
                            >
                                <div className="flex flex-1 items-center gap-4">
                                    <div className="flex items-center gap-2">
                                        <Circle
                                            className={`size-3 fill-current ${getStatusColor(connection.status)}`}
                                        />
                                        <Database className="size-5 text-gray-600 dark:text-gray-400" />
                                    </div>

                                    <div className="flex-1">
                                        <div className="flex items-center gap-2">
                                            <h4 className="font-medium">
                                                {connection.name}
                                            </h4>
                                            <Badge
                                                variant="outline"
                                                className="text-xs"
                                            >
                                                {getDatabaseTypeLabel(
                                                    connection.databaseType
                                                )}
                                            </Badge>
                                            {connection.environment && (
                                                <Badge
                                                    variant={
                                                        connection.environment ===
                                                        'production'
                                                            ? 'destructive'
                                                            : connection.environment ===
                                                                'staging'
                                                              ? 'default'
                                                              : 'secondary'
                                                    }
                                                    className="text-xs"
                                                >
                                                    {connection.environment}
                                                </Badge>
                                            )}
                                        </div>
                                        <p className="text-sm text-gray-500 dark:text-gray-400">
                                            {connection.host}:{connection.port}/
                                            {connection.databaseName}
                                        </p>
                                        <div className="mt-1 flex items-center gap-4 text-xs text-gray-400">
                                            <span
                                                className={getStatusColor(
                                                    connection.status
                                                )}
                                            >
                                                {getStatusLabel(
                                                    connection.status
                                                )}
                                            </span>
                                            {connection.lastTestedAt && (
                                                <span>
                                                    Last tested{' '}
                                                    {formatDistanceToNow(
                                                        new Date(
                                                            connection.lastTestedAt
                                                        ),
                                                        {
                                                            addSuffix: true,
                                                        }
                                                    )}
                                                </span>
                                            )}
                                        </div>
                                        {connection.lastError && (
                                            <p className="mt-1 text-xs text-red-500">
                                                {connection.lastError}
                                            </p>
                                        )}
                                    </div>
                                </div>

                                <div className="flex items-center gap-2">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={(e) =>
                                            handleRefresh(connection.id, e)
                                        }
                                        title="Test connection"
                                    >
                                        <RefreshCw className="size-4" />
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={(e) =>
                                            handleDelete(connection.id, e)
                                        }
                                        title="Delete connection"
                                    >
                                        <Trash2 className="size-4 text-red-500" />
                                    </Button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>
        </Card>
    );
};
