import React, { useCallback, useEffect, useState, useRef } from 'react';
import {
    collaborationContext,
    type CollaborationContextValue,
} from './collaboration-context';
import {
    wsService,
    type UserPresence,
    type DiagramEvent,
    type DiagramEventType,
    type CollaborationSession,
} from '@/services/api/websocket.service';
import { useAuth } from '@/context/auth-context';

export interface CollaborationProviderProps {
    children: React.ReactNode;
}

export const CollaborationProvider: React.FC<CollaborationProviderProps> = ({
    children,
}) => {
    const { isAuthenticated } = useAuth();
    const [isConnected, setIsConnected] = useState(false);
    const [isConnecting, setIsConnecting] = useState(false);
    const [activeUsers, setActiveUsers] = useState<UserPresence[]>([]);
    const [currentDiagramId, setCurrentDiagramId] = useState<string | null>(
        null
    );

    // Store event listeners in a ref to avoid stale closures
    const eventListenersRef = useRef<
        Map<DiagramEventType | '*', Set<(event: DiagramEvent) => void>>
    >(new Map());

    // Handle presence updates
    const handlePresenceUpdate = useCallback(
        (session: CollaborationSession) => {
            setActiveUsers(
                session.users.map((user) => ({
                    ...user,
                    lastSeen: new Date(user.lastSeen),
                }))
            );
        },
        []
    );

    // Handle diagram events
    const handleDiagramEvent = useCallback((event: DiagramEvent) => {
        // Notify specific listeners
        const specificListeners = eventListenersRef.current.get(event.type);
        if (specificListeners) {
            specificListeners.forEach((callback) => callback(event));
        }

        // Notify wildcard listeners
        const wildcardListeners = eventListenersRef.current.get('*');
        if (wildcardListeners) {
            wildcardListeners.forEach((callback) => callback(event));
        }
    }, []);

    // Set up WebSocket event handlers
    useEffect(() => {
        wsService.addPresenceListener(handlePresenceUpdate);
        wsService.addEventListener('*', handleDiagramEvent);

        return () => {
            wsService.removePresenceListener(handlePresenceUpdate);
            wsService.removeEventListener('*', handleDiagramEvent);
        };
    }, [handlePresenceUpdate, handleDiagramEvent]);

    // Update connection state and handle reconnection
    useEffect(() => {
        const checkConnection = () => {
            const connected = wsService.isConnected();
            if (connected !== isConnected) {
                console.log(
                    '[Collaboration] Connection state changed:',
                    connected
                );
                setIsConnected(connected);
            }

            // Auto-reconnect if we were connected to a diagram but lost connection
            if (
                !connected &&
                currentDiagramId &&
                isAuthenticated &&
                !isConnecting
            ) {
                console.log(
                    '[Collaboration] Lost connection, attempting to reconnect...'
                );
                reconnect();
            }
        };

        // Check connection periodically
        const interval = setInterval(checkConnection, 2000);
        checkConnection();

        return () => clearInterval(interval);
    }, [isConnected, currentDiagramId, isAuthenticated, isConnecting]);

    // Reconnect function
    const reconnect = async () => {
        if (isConnecting) return;

        setIsConnecting(true);
        try {
            await wsService.connect();
            setIsConnected(true);

            // Resubscribe to current diagram
            if (currentDiagramId) {
                wsService.subscribeToDiagram(currentDiagramId);
                console.log(
                    '[Collaboration] Reconnected and resubscribed to diagram:',
                    currentDiagramId
                );
            }
        } catch (error) {
            console.error('[Collaboration] Reconnection failed:', error);
        } finally {
            setIsConnecting(false);
        }
    };

    // Connect WebSocket when authenticated
    useEffect(() => {
        const connect = async () => {
            if (isAuthenticated && !wsService.isConnected()) {
                setIsConnecting(true);
                try {
                    await wsService.connect();
                    setIsConnected(true);
                } catch (error) {
                    console.error('Failed to connect WebSocket:', error);
                } finally {
                    setIsConnecting(false);
                }
            }
        };

        connect();
    }, [isAuthenticated]);

    // Disconnect when not authenticated
    useEffect(() => {
        if (!isAuthenticated && wsService.isConnected()) {
            wsService.disconnect();
            setIsConnected(false);
            setActiveUsers([]);
            setCurrentDiagramId(null);
        }
    }, [isAuthenticated]);

    const joinDiagram = useCallback(async (diagramId: string) => {
        console.log('[Collaboration] joinDiagram called:', diagramId);

        if (!wsService.isConnected()) {
            setIsConnecting(true);
            try {
                await wsService.connect();
                setIsConnected(true);
                console.log('[Collaboration] WebSocket connected');
            } catch (error) {
                console.error('Failed to connect WebSocket:', error);
                setIsConnecting(false);
                return;
            } finally {
                setIsConnecting(false);
            }
        } else {
            // Make sure state reflects actual connection
            setIsConnected(true);
        }

        wsService.subscribeToDiagram(diagramId);
        setCurrentDiagramId(diagramId);
        console.log(
            '[Collaboration] Subscribed to diagram:',
            diagramId,
            'isConnected:',
            wsService.isConnected()
        );
    }, []);

    const leaveDiagram = useCallback(() => {
        if (currentDiagramId) {
            wsService.leaveDiagram(currentDiagramId);
            setCurrentDiagramId(null);
            setActiveUsers([]);
        }
    }, [currentDiagramId]);

    const sendCursorPosition = useCallback(
        (x: number, y: number) => {
            if (currentDiagramId) {
                wsService.sendCursorPosition(currentDiagramId, x, y);
            }
        },
        [currentDiagramId]
    );

    const sendSelectionChange = useCallback(
        (elementId: string | null) => {
            if (currentDiagramId) {
                wsService.sendSelectionChange(currentDiagramId, elementId);
            }
        },
        [currentDiagramId]
    );

    const lockElement = useCallback(
        (elementType: string, elementId: string) => {
            if (currentDiagramId) {
                wsService.lockElement(currentDiagramId, elementType, elementId);
            }
        },
        [currentDiagramId]
    );

    const unlockElement = useCallback(
        (elementType: string, elementId: string) => {
            if (currentDiagramId) {
                wsService.unlockElement(
                    currentDiagramId,
                    elementType,
                    elementId
                );
            }
        },
        [currentDiagramId]
    );

    const subscribe = useCallback(
        (
            eventType: DiagramEventType | '*',
            callback: (event: DiagramEvent) => void
        ) => {
            if (!eventListenersRef.current.has(eventType)) {
                eventListenersRef.current.set(eventType, new Set());
            }
            eventListenersRef.current.get(eventType)!.add(callback);

            // Return unsubscribe function
            return () => {
                const listeners = eventListenersRef.current.get(eventType);
                if (listeners) {
                    listeners.delete(callback);
                }
            };
        },
        []
    );

    const value: CollaborationContextValue = {
        isConnected,
        isConnecting,
        activeUsers,
        currentDiagramId,
        joinDiagram,
        leaveDiagram,
        sendCursorPosition,
        sendSelectionChange,
        lockElement,
        unlockElement,
        subscribe,
    };

    return (
        <collaborationContext.Provider value={value}>
            {children}
        </collaborationContext.Provider>
    );
};

export default CollaborationProvider;
