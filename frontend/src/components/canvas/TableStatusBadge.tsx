import React from 'react';
import { Badge } from '@/components/badge/badge';
import { Circle, AlertTriangle, Check, Clock } from 'lucide-react';

export enum TableStatus {
    PLANNED = 'PLANNED',
    EXISTS = 'EXISTS',
    SYNCED = 'SYNCED',
    DRIFT = 'DRIFT',
}

interface TableStatusBadgeProps {
    status: TableStatus;
    showLabel?: boolean;
    size?: 'sm' | 'md' | 'lg';
}

const STATUS_CONFIG = {
    [TableStatus.PLANNED]: {
        icon: Clock,
        color: 'text-gray-400',
        bgColor: 'bg-gray-100 dark:bg-gray-800',
        borderColor: 'border-gray-300',
        label: 'Planned',
        description: 'This table only exists in the diagram',
    },
    [TableStatus.EXISTS]: {
        icon: Circle,
        color: 'text-yellow-500',
        bgColor: 'bg-yellow-50 dark:bg-yellow-950',
        borderColor: 'border-yellow-300',
        label: 'Exists',
        description: 'This table exists in the database',
    },
    [TableStatus.SYNCED]: {
        icon: Check,
        color: 'text-green-500',
        bgColor: 'bg-green-50 dark:bg-green-950',
        borderColor: 'border-green-300',
        label: 'Synced',
        description: 'Diagram matches the database schema',
    },
    [TableStatus.DRIFT]: {
        icon: AlertTriangle,
        color: 'text-red-500',
        bgColor: 'bg-red-50 dark:bg-red-950',
        borderColor: 'border-red-300',
        label: 'Drift',
        description: 'Schema has drifted from the diagram',
    },
};

export const TableStatusBadge: React.FC<TableStatusBadgeProps> = ({
    status,
    showLabel = true,
    size = 'sm',
}) => {
    const config = STATUS_CONFIG[status];
    const Icon = config.icon;

    const iconSizes = {
        sm: 'h-3 w-3',
        md: 'h-4 w-4',
        lg: 'h-5 w-5',
    };

    if (!showLabel) {
        return (
            <div
                className={`${config.color}`}
                title={`${config.label}: ${config.description}`}
            >
                <Icon className={iconSizes[size]} />
            </div>
        );
    }

    return (
        <Badge
            variant="outline"
            className={`${config.bgColor} ${config.borderColor} ${config.color} text-xs font-medium`}
            title={config.description}
        >
            <Icon className={`mr-1 ${iconSizes[size]}`} />
            {config.label}
        </Badge>
    );
};

interface TableStatusIndicatorProps {
    status: TableStatus;
    lastSyncedAt?: string;
    driftDetails?: string;
}

export const TableStatusIndicator: React.FC<TableStatusIndicatorProps> = ({
    status,
    lastSyncedAt,
    driftDetails,
}) => {
    const config = STATUS_CONFIG[status];
    const Icon = config.icon;

    return (
        <div
            className={`rounded-lg border p-2 ${config.borderColor} ${config.bgColor}`}
        >
            <div className="mb-1 flex items-center gap-2">
                <Icon className={`size-4 ${config.color}`} />
                <span className={`text-sm font-medium ${config.color}`}>
                    {config.label}
                </span>
            </div>
            <p className="text-xs text-gray-600 dark:text-gray-400">
                {config.description}
            </p>
            {lastSyncedAt && (
                <p className="mt-1 text-xs text-gray-500">
                    Last synced: {new Date(lastSyncedAt).toLocaleString()}
                </p>
            )}
            {driftDetails && status === TableStatus.DRIFT && (
                <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                    {driftDetails}
                </p>
            )}
        </div>
    );
};

// Utility function to get status color for canvas nodes
export const getTableStatusColor = (status: TableStatus): string => {
    switch (status) {
        case TableStatus.PLANNED:
            return '#9CA3AF'; // gray
        case TableStatus.EXISTS:
            return '#EAB308'; // yellow
        case TableStatus.SYNCED:
            return '#22C55E'; // green
        case TableStatus.DRIFT:
            return '#EF4444'; // red
        default:
            return '#9CA3AF';
    }
};
