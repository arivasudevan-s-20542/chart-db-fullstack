import React, { useEffect, useState } from 'react';
import { useCollaboration } from '@/context/collaboration-context';
import { useAuth } from '@/context/auth-context';
import { useReactFlow } from '@xyflow/react';

interface CursorPosition {
    userId: string;
    userName: string;
    color: string;
    x: number;
    y: number;
}

export const CursorsOverlay: React.FC = () => {
    const { activeUsers, currentDiagramId, subscribe } = useCollaboration();
    const { user } = useAuth();
    const { flowToScreenPosition } = useReactFlow();
    const [cursors, setCursors] = useState<Map<string, CursorPosition>>(new Map());

    useEffect(() => {
        console.log('[CursorsOverlay] Effect running, currentDiagramId:', currentDiagramId);
        if (!currentDiagramId) return;

        // Subscribe to cursor move events
        const unsubscribe = subscribe('CURSOR_MOVED', (event) => {
            console.log('[CursorsOverlay] CURSOR_MOVED event received:', event);
            if (event.userId === user?.id) return; // Don't show own cursor

            setCursors((prev) => {
                const newCursors = new Map(prev);
                newCursors.set(event.userId, {
                    userId: event.userId,
                    userName: event.payload?.userDisplayName || event.userEmail || 'Unknown',
                    color: event.payload?.cursorColor || stringToColor(event.userId),
                    x: event.payload?.x ?? 0,
                    y: event.payload?.y ?? 0,
                });
                console.log('[CursorsOverlay] Updated cursors:', newCursors.size);
                return newCursors;
            });
        });

        // Clean up cursors when users leave
        const unsubscribeLeft = subscribe('USER_LEFT', (event) => {
            setCursors((prev) => {
                const newCursors = new Map(prev);
                newCursors.delete(event.userId);
                return newCursors;
            });
        });

        return () => {
            unsubscribe();
            unsubscribeLeft();
        };
    }, [currentDiagramId, subscribe, user?.id]);

    // Update cursors from activeUsers when they have cursor positions
    useEffect(() => {
        activeUsers.forEach((u) => {
            if (u.userId !== user?.id && u.cursorX !== undefined && u.cursorY !== undefined) {
                setCursors((prev) => {
                    const newCursors = new Map(prev);
                    newCursors.set(u.userId, {
                        userId: u.userId,
                        userName: u.name || u.email || 'Unknown',
                        color: u.color || stringToColor(u.userId),
                        x: u.cursorX!,
                        y: u.cursorY!,
                    });
                    return newCursors;
                });
            }
        });
    }, [activeUsers, user?.id]);

    // Remove cursors for users no longer active
    useEffect(() => {
        const activeUserIds = new Set(activeUsers.map((u) => u.userId));
        setCursors((prev) => {
            const newCursors = new Map(prev);
            for (const userId of newCursors.keys()) {
                if (!activeUserIds.has(userId)) {
                    newCursors.delete(userId);
                }
            }
            return newCursors;
        });
    }, [activeUsers]);

    if (!currentDiagramId || cursors.size === 0) {
        return null;
    }

    return (
        <div className="pointer-events-none absolute inset-0 z-50 overflow-hidden">
            {Array.from(cursors.values()).map((cursor) => {
                // Transform flow coordinates to screen coordinates
                const screenPos = flowToScreenPosition({ x: cursor.x, y: cursor.y });
                return (
                    <Cursor
                        key={cursor.userId}
                        cursor={cursor}
                        screenX={screenPos.x}
                        screenY={screenPos.y}
                    />
                );
            })}
        </div>
    );
};

interface CursorProps {
    cursor: CursorPosition;
    screenX: number;
    screenY: number;
}

const Cursor: React.FC<CursorProps> = ({ cursor, screenX, screenY }) => {
    return (
        <div
            className="absolute transition-all duration-75 ease-out"
            style={{
                left: screenX,
                top: screenY,
                transform: 'translate(-2px, -2px)',
            }}
        >
            {/* Cursor arrow SVG */}
            <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="drop-shadow-md"
            >
                <path
                    d="M5.65376 12.4562L12.0001 2L18.3464 12.4562H5.65376Z"
                    fill={cursor.color}
                    stroke="white"
                    strokeWidth="1.5"
                    transform="rotate(-45 12 12)"
                />
            </svg>
            {/* User name label */}
            <div
                className="ml-4 mt-1 whitespace-nowrap rounded px-2 py-0.5 text-xs font-medium text-white shadow-md"
                style={{ backgroundColor: cursor.color }}
            >
                {cursor.userName}
            </div>
        </div>
    );
};

// Generate a consistent color from a string
const stringToColor = (str: string): string => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = hash % 360;
    return `hsl(${hue}, 70%, 50%)`;
};

export default CursorsOverlay;
