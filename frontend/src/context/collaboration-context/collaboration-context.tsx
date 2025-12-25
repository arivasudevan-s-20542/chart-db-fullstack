import { createContext } from 'react';
import type {
    UserPresence,
    DiagramEvent,
    DiagramEventType,
} from '@/services/api/websocket.service';

export interface CollaborationContextValue {
    // Connection state
    isConnected: boolean;
    isConnecting: boolean;

    // Presence
    activeUsers: UserPresence[];
    currentDiagramId: string | null;

    // Actions
    joinDiagram: (diagramId: string) => Promise<void>;
    leaveDiagram: () => void;
    sendCursorPosition: (x: number, y: number) => void;
    sendSelectionChange: (elementId: string | null) => void;
    lockElement: (elementType: string, elementId: string) => void;
    unlockElement: (elementType: string, elementId: string) => void;

    // Event handling
    subscribe: (
        eventType: DiagramEventType | '*',
        callback: (event: DiagramEvent) => void
    ) => () => void;
}

export const collaborationContext = createContext<CollaborationContextValue>({
    isConnected: false,
    isConnecting: false,
    activeUsers: [],
    currentDiagramId: null,
    joinDiagram: async () => {},
    leaveDiagram: () => {},
    sendCursorPosition: () => {},
    sendSelectionChange: () => {},
    lockElement: () => {},
    unlockElement: () => {},
    subscribe: () => () => {},
});

export default collaborationContext;
