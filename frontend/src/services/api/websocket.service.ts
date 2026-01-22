import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken } from './token-storage';

const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export type DiagramEventType =
    | 'TABLE_CREATED'
    | 'TABLE_UPDATED'
    | 'TABLE_DELETED'
    | 'COLUMN_CREATED'
    | 'COLUMN_UPDATED'
    | 'COLUMN_DELETED'
    | 'INDEX_CREATED'
    | 'INDEX_UPDATED'
    | 'INDEX_DELETED'
    | 'RELATIONSHIP_CREATED'
    | 'RELATIONSHIP_UPDATED'
    | 'RELATIONSHIP_DELETED'
    | 'AREA_CREATED'
    | 'AREA_UPDATED'
    | 'AREA_DELETED'
    | 'NOTE_CREATED'
    | 'NOTE_UPDATED'
    | 'NOTE_DELETED'
    | 'DIAGRAM_UPDATED'
    | 'USER_JOINED'
    | 'USER_LEFT'
    | 'CURSOR_MOVED'
    | 'SELECTION_CHANGED'
    | 'ELEMENT_LOCKED'
    | 'ELEMENT_UNLOCKED'
    | 'AI_ACTION';

export interface DiagramEvent {
    type: DiagramEventType;
    diagramId: string;
    userId: string;
    userEmail?: string;
    sessionId?: string;
    payload: any;
    timestamp: string;
}

export interface UserPresence {
    userId: string;
    email: string;
    name?: string;
    cursorX?: number;
    cursorY?: number;
    selectedElementId?: string;
    color?: string;
    lastSeen: Date;
}

export interface CollaborationSession {
    diagramId: string;
    users: UserPresence[];
}

type EventCallback = (event: DiagramEvent) => void;
type PresenceCallback = (session: CollaborationSession) => void;
type LatencyCallback = (latency: number) => void;

class WebSocketService {
    private client: Client | null = null;
    private subscriptions: Map<string, StompSubscription> = new Map();
    private eventListeners: Map<string, Set<EventCallback>> = new Map();
    private presenceListeners: Set<PresenceCallback> = new Set();
    private latencyListeners: Set<LatencyCallback> = new Set();
    private currentDiagramId: string | null = null;
    private sessionId: string | null = null;
    private latencyInterval: ReturnType<typeof setInterval> | null = null;
    private currentLatency: number = -1;
    private pendingPings: Map<string, number> = new Map();
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    private reconnectDelay = 2000;

    /**
     * Connect to WebSocket server
     */
    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.client?.connected) {
                resolve();
                return;
            }

            const token = getAccessToken();
            if (!token) {
                reject(new Error('Not authenticated'));
                return;
            }

            this.client = new Client({
                webSocketFactory: () => new SockJS(WS_BASE_URL),
                connectHeaders: {
                    Authorization: `Bearer ${token}`,
                },
                debug: (str) => {
                    if (import.meta.env.DEV) {
                        console.log('[WebSocket]', str);
                    }
                },
                reconnectDelay: this.reconnectDelay,
                heartbeatIncoming: 10000,
                heartbeatOutgoing: 10000,
            });

            this.client.onConnect = (frame) => {
                console.log('[WebSocket] Connected successfully');
                this.reconnectAttempts = 0;
                
                // Store session ID from STOMP connection
                this.sessionId = frame.headers['session'] || `session-${Date.now()}-${Math.random()}`;

                // Subscribe to pong responses for latency measurement
                this.subscribeToPong();

                // Resubscribe to diagram if we were connected before
                if (this.currentDiagramId) {
                    console.log(
                        '[WebSocket] Resubscribing to diagram:',
                        this.currentDiagramId
                    );
                    this.subscribeToDiagram(this.currentDiagramId);
                }

                // Start latency measurement
                this.startLatencyMeasurement();

                resolve();
            };

            this.client.onStompError = (frame) => {
                console.error(
                    '[WebSocket] STOMP error:',
                    frame.headers['message'],
                    frame
                );
                reject(new Error(frame.headers['message']));
            };

            this.client.onWebSocketClose = (event) => {
                console.log(
                    '[WebSocket] Connection closed. Code:',
                    event?.code,
                    'Reason:',
                    event?.reason
                );
                this.reconnectAttempts++;

                if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                    console.error('[WebSocket] Max reconnect attempts reached');
                }
            };

            this.client.onDisconnect = () => {
                console.log('[WebSocket] Disconnected from STOMP broker');
            };

            this.client.activate();
        });
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect(): void {
        if (this.currentDiagramId) {
            this.leaveDiagram(this.currentDiagramId);
        }

        this.stopLatencyMeasurement();

        this.subscriptions.forEach((sub) => sub.unsubscribe());
        this.subscriptions.clear();
        this.eventListeners.clear();
        this.presenceListeners.clear();
        this.latencyListeners.clear();
        
        this.sessionId = null;

        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
    }

    /**
     * Subscribe to pong responses for latency measurement
     */
    private subscribeToPong(): void {
        if (!this.client?.connected) return;

        const pongSub = this.client.subscribe('/user/queue/pong', (message) => {
            try {
                const data = JSON.parse(message.body);
                if (data.pingId) {
                    this.handlePong(data.pingId);
                }
            } catch {
                // Ignore parse errors
            }
        });
        this.subscriptions.set('pong', pongSub);
    }

    /**
     * Subscribe to diagram events
     */
    subscribeToDiagram(diagramId: string): void {
        if (!this.client?.connected) {
            console.warn('[WebSocket] Not connected, queuing subscription');
            this.currentDiagramId = diagramId;
            return;
        }

        // Unsubscribe from previous diagram
        if (this.currentDiagramId && this.currentDiagramId !== diagramId) {
            this.unsubscribeFromDiagram(this.currentDiagramId);
        }

        this.currentDiagramId = diagramId;

        // Subscribe to diagram events
        const eventSub = this.client.subscribe(
            `/topic/diagram/${diagramId}/events`,
            (message: IMessage) => {
                const event: DiagramEvent = JSON.parse(message.body);
                this.notifyEventListeners(event);
            }
        );
        this.subscriptions.set(`events-${diagramId}`, eventSub);

        // Subscribe to presence updates
        const presenceSub = this.client.subscribe(
            `/topic/diagram/${diagramId}/presence`,
            (message: IMessage) => {
                const session: CollaborationSession = JSON.parse(message.body);
                this.notifyPresenceListeners(session);
            }
        );
        this.subscriptions.set(`presence-${diagramId}`, presenceSub);

        // Subscribe to cursor updates
        const cursorSub = this.client.subscribe(
            `/topic/diagram/${diagramId}/cursors`,
            (message: IMessage) => {
                const cursorData = JSON.parse(message.body);
                // Convert cursor broadcast to diagram event format
                const event: DiagramEvent = {
                    type: 'CURSOR_MOVED',
                    diagramId,
                    userId: cursorData.userId,
                    userEmail: cursorData.userDisplayName,
                    payload: {
                        x: cursorData.x,
                        y: cursorData.y,
                        userDisplayName: cursorData.userDisplayName,
                        userAvatarUrl: cursorData.userAvatarUrl,
                        cursorColor: cursorData.cursorColor,
                    },
                    timestamp: new Date().toISOString(),
                };
                this.notifyEventListeners(event);
            }
        );
        this.subscriptions.set(`cursors-${diagramId}`, cursorSub);

        // Subscribe to personal messages
        const personalSub = this.client.subscribe(
            '/user/queue/messages',
            (message: IMessage) => {
                const event: DiagramEvent = JSON.parse(message.body);
                this.notifyEventListeners(event);
            }
        );
        this.subscriptions.set('personal', personalSub);

        // Notify server that we joined
        this.joinDiagram(diagramId);
    }

    /**
     * Unsubscribe from diagram
     */
    unsubscribeFromDiagram(diagramId: string): void {
        const eventSub = this.subscriptions.get(`events-${diagramId}`);
        if (eventSub) {
            eventSub.unsubscribe();
            this.subscriptions.delete(`events-${diagramId}`);
        }

        const presenceSub = this.subscriptions.get(`presence-${diagramId}`);
        if (presenceSub) {
            presenceSub.unsubscribe();
            this.subscriptions.delete(`presence-${diagramId}`);
        }

        const cursorSub = this.subscriptions.get(`cursors-${diagramId}`);
        if (cursorSub) {
            cursorSub.unsubscribe();
            this.subscriptions.delete(`cursors-${diagramId}`);
        }

        if (this.currentDiagramId === diagramId) {
            this.currentDiagramId = null;
        }
    }

    /**
     * Join diagram session
     */
    private joinDiagram(diagramId: string): void {
        this.sendMessage(`/app/diagram/${diagramId}/join`, {});
    }

    /**
     * Leave diagram session
     */
    leaveDiagram(diagramId: string): void {
        this.sendMessage(`/app/diagram/${diagramId}/leave`, {});
        this.unsubscribeFromDiagram(diagramId);
    }

    /**
     * Send cursor position update
     */
    sendCursorPosition(diagramId: string, x: number, y: number): void {
        this.sendMessage(`/app/diagram/${diagramId}/cursor`, { x, y });
    }

    /**
     * Send selection change
     */
    sendSelectionChange(diagramId: string, elementId: string | null): void {
        this.sendMessage(`/app/diagram/${diagramId}/selection`, { elementId });
    }

    /**
     * Lock an element for editing
     */
    lockElement(
        diagramId: string,
        elementType: string,
        elementId: string
    ): void {
        this.sendMessage(`/app/diagram/${diagramId}/lock`, {
            elementType,
            elementId,
        });
    }

    /**
     * Unlock an element
     */
    unlockElement(
        diagramId: string,
        elementType: string,
        elementId: string
    ): void {
        this.sendMessage(`/app/diagram/${diagramId}/unlock`, {
            elementType,
            elementId,
        });
    }

    /**
     * Send a diagram event (for real-time updates)
     */
    sendDiagramEvent(
        diagramId: string,
        type: DiagramEventType,
        payload: any
    ): void {
        this.sendMessage(`/app/diagram/${diagramId}/event`, { type, payload });
    }

    /**
     * Send message via WebSocket
     */
    private sendMessage(destination: string, body: any): void {
        if (!this.client?.connected) {
            console.warn('[WebSocket] Not connected, cannot send message');
            return;
        }

        this.client.publish({
            destination,
            body: JSON.stringify(body),
        });
    }

    /**
     * Add event listener
     */
    addEventListener(
        eventType: DiagramEventType | '*',
        callback: EventCallback
    ): void {
        const key = eventType;
        if (!this.eventListeners.has(key)) {
            this.eventListeners.set(key, new Set());
        }
        this.eventListeners.get(key)!.add(callback);
    }

    /**
     * Remove event listener
     */
    removeEventListener(
        eventType: DiagramEventType | '*',
        callback: EventCallback
    ): void {
        const listeners = this.eventListeners.get(eventType);
        if (listeners) {
            listeners.delete(callback);
        }
    }

    /**
     * Add presence listener
     */
    addPresenceListener(callback: PresenceCallback): void {
        this.presenceListeners.add(callback);
    }

    /**
     * Remove presence listener
     */
    removePresenceListener(callback: PresenceCallback): void {
        this.presenceListeners.delete(callback);
    }

    /**
     * Notify event listeners
     */
    private notifyEventListeners(event: DiagramEvent): void {
        // Notify specific listeners
        const specificListeners = this.eventListeners.get(event.type);
        if (specificListeners) {
            specificListeners.forEach((callback) => callback(event));
        }

        // Notify wildcard listeners
        const wildcardListeners = this.eventListeners.get('*');
        if (wildcardListeners) {
            wildcardListeners.forEach((callback) => callback(event));
        }
    }

    /**
     * Notify presence listeners
     */
    private notifyPresenceListeners(session: CollaborationSession): void {
        this.presenceListeners.forEach((callback) => callback(session));
    }

    /**
     * Start latency measurement (ping every 5 seconds)
     */
    private startLatencyMeasurement(): void {
        this.stopLatencyMeasurement();
        this.measureLatency();
        this.latencyInterval = setInterval(() => {
            this.measureLatency();
        }, 5000);
    }

    /**
     * Stop latency measurement
     */
    private stopLatencyMeasurement(): void {
        if (this.latencyInterval) {
            clearInterval(this.latencyInterval);
            this.latencyInterval = null;
        }
        this.currentLatency = -1;
    }

    /**
     * Measure round-trip latency using WebSocket ping/pong
     */
    private measureLatency(): void {
        if (!this.client?.connected || !this.currentDiagramId) {
            return;
        }

        const pingId = Date.now().toString();
        this.pendingPings.set(pingId, performance.now());

        // Send ping via WebSocket
        this.sendMessage(`/app/ping`, { pingId });

        // Timeout - if no pong received in 5 seconds, consider it failed
        setTimeout(() => {
            if (this.pendingPings.has(pingId)) {
                this.pendingPings.delete(pingId);
            }
        }, 5000);
    }

    /**
     * Handle pong response from server
     */
    private handlePong(pingId: string): void {
        const startTime = this.pendingPings.get(pingId);
        if (startTime) {
            const latency = Math.round(performance.now() - startTime);
            this.currentLatency = latency;
            this.pendingPings.delete(pingId);
            this.notifyLatencyListeners(latency);
        }
    }

    /**
     * Notify latency listeners
     */
    private notifyLatencyListeners(latency: number): void {
        this.latencyListeners.forEach((callback) => callback(latency));
    }

    /**
     * Add latency listener
     */
    addLatencyListener(callback: LatencyCallback): void {
        this.latencyListeners.add(callback);
        // Immediately notify with current latency if available
        if (this.currentLatency >= 0) {
            callback(this.currentLatency);
        }
    }

    /**
     * Remove latency listener
     */
    removeLatencyListener(callback: LatencyCallback): void {
        this.latencyListeners.delete(callback);
    }

    /**
     * Get current latency
     */
    getLatency(): number {
        return this.currentLatency;
    }

    /**
     * Check if connected
     */
    isConnected(): boolean {
        return this.client?.connected ?? false;
    }

    /**
     * Get current diagram ID
     */
    getCurrentDiagramId(): string | null {
        return this.currentDiagramId;
    }

    /**
     * Get current WebSocket session ID
     */
    getSessionId(): string | null {
        return this.sessionId;
    }
}

// Singleton instance
export const wsService = new WebSocketService();

export default wsService;
