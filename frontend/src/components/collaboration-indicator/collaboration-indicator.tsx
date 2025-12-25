import React from 'react';
import { useCollaboration } from '@/context/collaboration-context';
import { Avatar, AvatarFallback } from '@/components/avatar/avatar';
import {
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@/components/tooltip/tooltip';
import { Wifi, WifiOff, Users, Activity } from 'lucide-react';

// Generate a consistent color from a string
const stringToColor = (str: string) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = hash % 360;
    return `hsl(${hue}, 70%, 50%)`;
};

// Get latency color based on value
const getLatencyColor = (latency: number) => {
    if (latency < 0) return 'text-muted-foreground';
    if (latency < 100) return 'text-green-500';
    if (latency < 300) return 'text-yellow-500';
    return 'text-red-500';
};

export const CollaborationIndicator: React.FC = () => {
    const { isConnected, isConnecting, activeUsers, currentDiagramId, latency } =
        useCollaboration();

    // Don't show if not in a diagram session
    if (!currentDiagramId) {
        return null;
    }

    return (
        <div className="flex items-center gap-2">
            {/* Connection status */}
            <Tooltip>
                <TooltipTrigger asChild>
                    <div className="flex items-center">
                        {isConnecting ? (
                            <Wifi className="size-4 animate-pulse text-yellow-500" />
                        ) : isConnected ? (
                            <Wifi className="size-4 text-green-500" />
                        ) : (
                            <WifiOff className="size-4 text-destructive" />
                        )}
                    </div>
                </TooltipTrigger>
                <TooltipContent>
                    {isConnecting
                        ? 'Connecting...'
                        : isConnected
                          ? 'Real-time collaboration active'
                          : 'Not connected'}
                </TooltipContent>
            </Tooltip>

            {/* Latency indicator */}
            {isConnected && latency >= 0 && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className={`flex items-center gap-1 ${getLatencyColor(latency)}`}>
                            <Activity className="size-3" />
                            <span className="text-xs font-medium tabular-nums">
                                {latency}ms
                            </span>
                        </div>
                    </TooltipTrigger>
                    <TooltipContent>
                        <div className="text-sm">
                            <div className="font-medium">Server Latency</div>
                            <div className="text-xs text-muted-foreground">
                                {latency < 100
                                    ? 'Excellent connection'
                                    : latency < 300
                                      ? 'Good connection'
                                      : 'High latency'}
                            </div>
                        </div>
                    </TooltipContent>
                </Tooltip>
            )}

            {/* Active users */}
            {activeUsers.length > 0 && (
                <div className="flex items-center gap-1">
                    <Users className="size-4 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">
                        {activeUsers.length}
                    </span>
                    <div className="ml-1 flex -space-x-2">
                        {activeUsers.slice(0, 5).map((user) => {
                            const initials = user.name
                                ? user.name
                                      .split(' ')
                                      .map((n) => n[0])
                                      .join('')
                                      .toUpperCase()
                                      .slice(0, 2)
                                : user.email[0].toUpperCase();

                            const bgColor =
                                user.color || stringToColor(user.userId);

                            return (
                                <Tooltip key={user.userId}>
                                    <TooltipTrigger asChild>
                                        <Avatar className="size-6 border-2 border-background">
                                            <AvatarFallback
                                                style={{
                                                    backgroundColor: bgColor,
                                                }}
                                                className="text-xs text-white"
                                            >
                                                {initials}
                                            </AvatarFallback>
                                        </Avatar>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                        <div className="text-sm">
                                            <div className="font-medium">
                                                {user.name || user.email}
                                            </div>
                                            {user.selectedElementId && (
                                                <div className="text-xs text-muted-foreground">
                                                    Editing:{' '}
                                                    {user.selectedElementId}
                                                </div>
                                            )}
                                        </div>
                                    </TooltipContent>
                                </Tooltip>
                            );
                        })}
                        {activeUsers.length > 5 && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Avatar className="size-6 border-2 border-background">
                                        <AvatarFallback className="bg-muted text-xs">
                                            +{activeUsers.length - 5}
                                        </AvatarFallback>
                                    </Avatar>
                                </TooltipTrigger>
                                <TooltipContent>
                                    <div className="text-sm">
                                        {activeUsers.slice(5).map((user) => (
                                            <div key={user.userId}>
                                                {user.name || user.email}
                                            </div>
                                        ))}
                                    </div>
                                </TooltipContent>
                            </Tooltip>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default CollaborationIndicator;
