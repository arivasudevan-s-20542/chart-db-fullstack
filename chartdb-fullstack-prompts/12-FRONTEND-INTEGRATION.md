# 12 - Frontend Integration

## 🌐 React + WebSocket Integration

### 1. WebSocket Service

```typescript
// src/services/websocket.service.ts
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { EventEmitter } from 'events';

export interface WebSocketConfig {
  url: string;
  token: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
}

export interface CursorPosition {
  userId: string;
  userName: string;
  x: number;
  y: number;
  color: string;
}

export interface Collaborator {
  userId: string;
  userName: string;
  avatarUrl?: string;
  cursorColor: string;
  joinedAt: string;
}

class WebSocketService extends EventEmitter {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private isConnected: boolean = false;
  private currentDiagramId: string | null = null;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 10;

  connect(config: WebSocketConfig): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve();
        return;
      }

      this.client = new Client({
        webSocketFactory: () => new SockJS(config.url),
        connectHeaders: {
          Authorization: `Bearer ${config.token}`,
        },
        debug: (str) => {
          if (process.env.NODE_ENV === 'development') {
            console.log('[STOMP]', str);
          }
        },
        reconnectDelay: config.reconnectDelay ?? 5000,
        heartbeatIncoming: config.heartbeatIncoming ?? 4000,
        heartbeatOutgoing: config.heartbeatOutgoing ?? 4000,
        onConnect: () => {
          this.isConnected = true;
          this.reconnectAttempts = 0;
          this.emit('connected');
          resolve();
        },
        onDisconnect: () => {
          this.isConnected = false;
          this.emit('disconnected');
        },
        onStompError: (frame) => {
          console.error('[STOMP Error]', frame.headers['message']);
          this.emit('error', frame);
          reject(new Error(frame.headers['message']));
        },
        onWebSocketError: (event) => {
          console.error('[WebSocket Error]', event);
          this.emit('error', event);
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.currentDiagramId) {
      this.leaveDiagram(this.currentDiagramId);
    }
    
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    
    this.isConnected = false;
  }

  // ═══════════════════════════════════════════════════════════════
  // DIAGRAM COLLABORATION
  // ═══════════════════════════════════════════════════════════════

  async joinDiagram(diagramId: string): Promise<Collaborator[]> {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    this.currentDiagramId = diagramId;

    // Subscribe to all diagram events
    this.subscribeToDiagram(diagramId);

    // Send join message and wait for response
    return new Promise((resolve) => {
      const subscription = this.client!.subscribe(
        '/user/queue/join-result',
        (message: IMessage) => {
          subscription.unsubscribe();
          resolve(JSON.parse(message.body));
        }
      );

      this.client!.publish({
        destination: '/app/join-diagram',
        body: JSON.stringify({ diagramId }),
      });
    });
  }

  leaveDiagram(diagramId: string): void {
    if (this.client?.connected) {
      this.client.publish({
        destination: '/app/leave-diagram',
        body: JSON.stringify({ diagramId }),
      });
    }

    // Unsubscribe from diagram topics
    this.subscriptions.forEach((sub, key) => {
      if (key.includes(diagramId)) {
        sub.unsubscribe();
        this.subscriptions.delete(key);
      }
    });

    this.currentDiagramId = null;
  }

  private subscribeToDiagram(diagramId: string): void {
    const topics = [
      'table-created',
      'table-updated',
      'table-moved',
      'table-deleted',
      'column-created',
      'column-updated',
      'column-deleted',
      'relationship-created',
      'relationship-updated',
      'relationship-deleted',
      'cursor-update',
      'user-joined',
      'user-left',
      'table-locked',
      'table-unlocked',
      'selection-changed',
    ];

    topics.forEach((topic) => {
      const destination = `/topic/diagram/${diagramId}/${topic}`;
      
      if (!this.subscriptions.has(destination)) {
        const subscription = this.client!.subscribe(
          destination,
          (message: IMessage) => {
            const data = JSON.parse(message.body);
            this.emit(topic, data);
          }
        );
        this.subscriptions.set(destination, subscription);
      }
    });

    // Subscribe to user-specific error queue
    const errorDestination = '/user/queue/errors';
    if (!this.subscriptions.has(errorDestination)) {
      const errorSub = this.client!.subscribe(
        errorDestination,
        (message: IMessage) => {
          const error = JSON.parse(message.body);
          this.emit('error', error);
        }
      );
      this.subscriptions.set(errorDestination, errorSub);
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // CURSOR TRACKING
  // ═══════════════════════════════════════════════════════════════

  sendCursorPosition(diagramId: string, x: number, y: number): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/cursor-move',
      body: JSON.stringify({ diagramId, x, y }),
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // TABLE OPERATIONS
  // ═══════════════════════════════════════════════════════════════

  sendTableMove(diagramId: string, tableId: string, x: number, y: number): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/table-move',
      body: JSON.stringify({ diagramId, tableId, x, y }),
    });
  }

  sendTableCreate(diagramId: string, table: any): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/table-create',
      body: JSON.stringify({ diagramId, ...table }),
    });
  }

  sendTableUpdate(diagramId: string, tableId: string, updates: any): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/table-update',
      body: JSON.stringify({ diagramId, tableId, ...updates }),
    });
  }

  sendTableDelete(diagramId: string, tableId: string): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/table-delete',
      body: JSON.stringify({ diagramId, tableId }),
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // LOCKING
  // ═══════════════════════════════════════════════════════════════

  async requestTableLock(diagramId: string, tableId: string): Promise<boolean> {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    return new Promise((resolve) => {
      const subscription = this.client!.subscribe(
        '/user/queue/lock-result',
        (message: IMessage) => {
          subscription.unsubscribe();
          const result = JSON.parse(message.body);
          resolve(result.acquired);
        }
      );

      this.client!.publish({
        destination: '/app/lock-table',
        body: JSON.stringify({ diagramId, tableId }),
      });

      // Timeout after 5 seconds
      setTimeout(() => {
        subscription.unsubscribe();
        resolve(false);
      }, 5000);
    });
  }

  releaseTableLock(diagramId: string, tableId: string): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/unlock-table',
      body: JSON.stringify({ diagramId, tableId }),
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // SELECTION
  // ═══════════════════════════════════════════════════════════════

  sendSelection(diagramId: string, tableId?: string, columnId?: string): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination: '/app/select',
      body: JSON.stringify({
        diagramId,
        selectedTableId: tableId,
        selectedColumnId: columnId,
      }),
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // STATUS
  // ═══════════════════════════════════════════════════════════════

  get connected(): boolean {
    return this.isConnected;
  }
}

export const websocketService = new WebSocketService();
```

---

### 2. React Context for Collaboration

```typescript
// src/context/CollaborationContext.tsx
import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useRef,
} from 'react';
import { websocketService, Collaborator, CursorPosition } from '@/services/websocket.service';
import { useAuth } from './AuthContext';
import { useToast } from '@/hooks/useToast';

interface CollaborationContextType {
  isConnected: boolean;
  collaborators: Collaborator[];
  cursors: Map<string, CursorPosition>;
  lockedTables: Map<string, { userId: string; userName: string }>;
  joinDiagram: (diagramId: string) => Promise<void>;
  leaveDiagram: () => void;
  updateCursor: (x: number, y: number) => void;
  lockTable: (tableId: string) => Promise<boolean>;
  unlockTable: (tableId: string) => void;
  updateSelection: (tableId?: string, columnId?: string) => void;
}

const CollaborationContext = createContext<CollaborationContextType | undefined>(
  undefined
);

export function CollaborationProvider({ children }: { children: React.ReactNode }) {
  const { user, token } = useAuth();
  const { toast } = useToast();
  
  const [isConnected, setIsConnected] = useState(false);
  const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
  const [cursors, setCursors] = useState<Map<string, CursorPosition>>(new Map());
  const [lockedTables, setLockedTables] = useState<Map<string, { userId: string; userName: string }>>(new Map());
  
  const diagramIdRef = useRef<string | null>(null);
  const cursorThrottleRef = useRef<NodeJS.Timeout | null>(null);

  // Connect to WebSocket when authenticated
  useEffect(() => {
    if (!token) return;

    const connect = async () => {
      try {
        await websocketService.connect({
          url: import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws',
          token,
        });
      } catch (error) {
        console.error('WebSocket connection failed:', error);
      }
    };

    connect();

    // Event listeners
    const handleConnected = () => setIsConnected(true);
    const handleDisconnected = () => setIsConnected(false);
    
    const handleUserJoined = (data: any) => {
      if (data.userId !== user?.id) {
        setCollaborators((prev) => {
          const exists = prev.some((c) => c.userId === data.userId);
          if (exists) return prev;
          
          toast({
            title: 'User joined',
            description: `${data.userName} is now viewing this diagram`,
          });
          
          return [...prev, {
            userId: data.userId,
            userName: data.userName,
            avatarUrl: data.avatarUrl,
            cursorColor: data.cursorColor,
            joinedAt: new Date().toISOString(),
          }];
        });
      }
    };
    
    const handleUserLeft = (data: any) => {
      setCollaborators((prev) => prev.filter((c) => c.userId !== data.userId));
      setCursors((prev) => {
        const next = new Map(prev);
        next.delete(data.userId);
        return next;
      });
    };
    
    const handleCursorUpdate = (data: CursorPosition) => {
      if (data.userId !== user?.id) {
        setCursors((prev) => {
          const next = new Map(prev);
          next.set(data.userId, data);
          return next;
        });
      }
    };
    
    const handleTableLocked = (data: any) => {
      setLockedTables((prev) => {
        const next = new Map(prev);
        next.set(data.tableId, {
          userId: data.userId,
          userName: data.userName,
        });
        return next;
      });
    };
    
    const handleTableUnlocked = (data: any) => {
      setLockedTables((prev) => {
        const next = new Map(prev);
        next.delete(data.tableId);
        return next;
      });
    };

    websocketService.on('connected', handleConnected);
    websocketService.on('disconnected', handleDisconnected);
    websocketService.on('user-joined', handleUserJoined);
    websocketService.on('user-left', handleUserLeft);
    websocketService.on('cursor-update', handleCursorUpdate);
    websocketService.on('table-locked', handleTableLocked);
    websocketService.on('table-unlocked', handleTableUnlocked);

    return () => {
      websocketService.off('connected', handleConnected);
      websocketService.off('disconnected', handleDisconnected);
      websocketService.off('user-joined', handleUserJoined);
      websocketService.off('user-left', handleUserLeft);
      websocketService.off('cursor-update', handleCursorUpdate);
      websocketService.off('table-locked', handleTableLocked);
      websocketService.off('table-unlocked', handleTableUnlocked);
      websocketService.disconnect();
    };
  }, [token, user?.id]);

  const joinDiagram = useCallback(async (diagramId: string) => {
    diagramIdRef.current = diagramId;
    const collaboratorList = await websocketService.joinDiagram(diagramId);
    setCollaborators(collaboratorList.filter((c) => c.userId !== user?.id));
  }, [user?.id]);

  const leaveDiagram = useCallback(() => {
    if (diagramIdRef.current) {
      websocketService.leaveDiagram(diagramIdRef.current);
      diagramIdRef.current = null;
      setCollaborators([]);
      setCursors(new Map());
      setLockedTables(new Map());
    }
  }, []);

  const updateCursor = useCallback((x: number, y: number) => {
    if (!diagramIdRef.current) return;

    // Throttle cursor updates to 50ms
    if (cursorThrottleRef.current) return;
    
    cursorThrottleRef.current = setTimeout(() => {
      cursorThrottleRef.current = null;
    }, 50);

    websocketService.sendCursorPosition(diagramIdRef.current, x, y);
  }, []);

  const lockTable = useCallback(async (tableId: string): Promise<boolean> => {
    if (!diagramIdRef.current) return false;
    return websocketService.requestTableLock(diagramIdRef.current, tableId);
  }, []);

  const unlockTable = useCallback((tableId: string) => {
    if (!diagramIdRef.current) return;
    websocketService.releaseTableLock(diagramIdRef.current, tableId);
  }, []);

  const updateSelection = useCallback((tableId?: string, columnId?: string) => {
    if (!diagramIdRef.current) return;
    websocketService.sendSelection(diagramIdRef.current, tableId, columnId);
  }, []);

  return (
    <CollaborationContext.Provider
      value={{
        isConnected,
        collaborators,
        cursors,
        lockedTables,
        joinDiagram,
        leaveDiagram,
        updateCursor,
        lockTable,
        unlockTable,
        updateSelection,
      }}
    >
      {children}
    </CollaborationContext.Provider>
  );
}

export function useCollaboration() {
  const context = useContext(CollaborationContext);
  if (!context) {
    throw new Error('useCollaboration must be used within CollaborationProvider');
  }
  return context;
}
```

---

### 3. Collaborator Cursor Component

```tsx
// src/components/collaboration/CollaboratorCursor.tsx
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CursorPosition } from '@/services/websocket.service';

interface CollaboratorCursorProps {
  cursor: CursorPosition;
}

export function CollaboratorCursor({ cursor }: CollaboratorCursorProps) {
  return (
    <motion.div
      className="pointer-events-none absolute z-50"
      initial={{ opacity: 0, scale: 0.5 }}
      animate={{ 
        opacity: 1, 
        scale: 1,
        x: cursor.x,
        y: cursor.y,
      }}
      exit={{ opacity: 0, scale: 0 }}
      transition={{ 
        type: 'spring', 
        damping: 30, 
        stiffness: 500,
        mass: 0.5,
      }}
    >
      {/* Cursor arrow */}
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        style={{ 
          filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.3))',
          transform: 'rotate(-5deg)',
        }}
      >
        <path
          d="M5.65376 3.05493L19.3787 11.5219C20.0768 11.9426 20.0405 12.9629 19.3119 13.3343L13.2743 16.4111L10.2398 22.2965C9.86521 23.0303 8.84SEQ 23.0303 8.47021 22.2965L5.43571 16.4111C5.28687 16.1131 5.03657 15.8764 4.72998 15.7424L1.59766 14.3808C0.868865 14.0658 0.821566 13.0455 1.50907 12.6648L5.65376 10.1977V3.05493Z"
          fill={cursor.color}
          stroke="white"
          strokeWidth="1.5"
        />
      </svg>
      
      {/* Name label */}
      <div
        className="ml-4 mt-1 whitespace-nowrap rounded px-2 py-0.5 text-xs font-medium text-white"
        style={{ backgroundColor: cursor.color }}
      >
        {cursor.userName}
      </div>
    </motion.div>
  );
}

// Cursor Overlay Component
export function CollaboratorCursorsOverlay() {
  const { cursors } = useCollaboration();
  
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <AnimatePresence>
        {Array.from(cursors.values()).map((cursor) => (
          <CollaboratorCursor key={cursor.userId} cursor={cursor} />
        ))}
      </AnimatePresence>
    </div>
  );
}
```

---

### 4. Collaborator Avatars Component

```tsx
// src/components/collaboration/CollaboratorAvatars.tsx
import React from 'react';
import { useCollaboration } from '@/context/CollaborationContext';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';

export function CollaboratorAvatars() {
  const { collaborators, isConnected } = useCollaboration();
  
  const maxVisible = 5;
  const visibleCollaborators = collaborators.slice(0, maxVisible);
  const remainingCount = collaborators.length - maxVisible;

  return (
    <div className="flex items-center gap-2">
      {/* Connection indicator */}
      <div className="flex items-center gap-1.5">
        <div
          className={`h-2 w-2 rounded-full ${
            isConnected ? 'bg-green-500' : 'bg-red-500'
          }`}
        />
        <span className="text-sm text-muted-foreground">
          {isConnected ? 'Live' : 'Offline'}
        </span>
      </div>
      
      {/* Collaborator avatars */}
      <div className="flex -space-x-2">
        {visibleCollaborators.map((collaborator) => (
          <Tooltip key={collaborator.userId}>
            <TooltipTrigger asChild>
              <Avatar
                className="h-8 w-8 border-2 border-background"
                style={{ borderColor: collaborator.cursorColor }}
              >
                <AvatarImage 
                  src={collaborator.avatarUrl} 
                  alt={collaborator.userName} 
                />
                <AvatarFallback
                  style={{ backgroundColor: collaborator.cursorColor }}
                  className="text-xs text-white"
                >
                  {collaborator.userName.slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
            </TooltipTrigger>
            <TooltipContent>
              <p>{collaborator.userName}</p>
              <p className="text-xs text-muted-foreground">Viewing</p>
            </TooltipContent>
          </Tooltip>
        ))}
        
        {remainingCount > 0 && (
          <Tooltip>
            <TooltipTrigger asChild>
              <Avatar className="h-8 w-8 border-2 border-background bg-muted">
                <AvatarFallback className="text-xs">
                  +{remainingCount}
                </AvatarFallback>
              </Avatar>
            </TooltipTrigger>
            <TooltipContent>
              <p>{remainingCount} more viewing</p>
            </TooltipContent>
          </Tooltip>
        )}
      </div>
    </div>
  );
}
```

---

### 5. Real-time Sync Hook

```typescript
// src/hooks/useRealtimeSync.ts
import { useEffect, useCallback } from 'react';
import { websocketService } from '@/services/websocket.service';
import { useDiagramStore } from '@/stores/diagramStore';
import { useAuth } from '@/context/AuthContext';

export function useRealtimeSync(diagramId: string | undefined) {
  const { user } = useAuth();
  const { 
    addTable, 
    updateTable, 
    removeTable,
    addColumn,
    updateColumn,
    removeColumn,
    addRelationship,
    removeRelationship,
  } = useDiagramStore();

  useEffect(() => {
    if (!diagramId) return;

    // Table events
    const handleTableCreated = (data: any) => {
      if (data.userId !== user?.id) {
        addTable(data);
      }
    };
    
    const handleTableUpdated = (data: any) => {
      if (data.userId !== user?.id) {
        updateTable(data.tableId, data);
      }
    };
    
    const handleTableMoved = (data: any) => {
      if (data.userId !== user?.id) {
        updateTable(data.tableId, {
          positionX: data.x,
          positionY: data.y,
        });
      }
    };
    
    const handleTableDeleted = (data: any) => {
      if (data.userId !== user?.id) {
        removeTable(data.tableId);
      }
    };

    // Column events
    const handleColumnCreated = (data: any) => {
      if (data.userId !== user?.id) {
        addColumn(data.tableId, data);
      }
    };
    
    const handleColumnUpdated = (data: any) => {
      if (data.userId !== user?.id) {
        updateColumn(data.tableId, data.columnId, data);
      }
    };
    
    const handleColumnDeleted = (data: any) => {
      if (data.userId !== user?.id) {
        removeColumn(data.tableId, data.columnId);
      }
    };

    // Relationship events
    const handleRelationshipCreated = (data: any) => {
      if (data.userId !== user?.id) {
        addRelationship(data);
      }
    };
    
    const handleRelationshipDeleted = (data: any) => {
      if (data.userId !== user?.id) {
        removeRelationship(data.relationshipId);
      }
    };

    // Subscribe to events
    websocketService.on('table-created', handleTableCreated);
    websocketService.on('table-updated', handleTableUpdated);
    websocketService.on('table-moved', handleTableMoved);
    websocketService.on('table-deleted', handleTableDeleted);
    websocketService.on('column-created', handleColumnCreated);
    websocketService.on('column-updated', handleColumnUpdated);
    websocketService.on('column-deleted', handleColumnDeleted);
    websocketService.on('relationship-created', handleRelationshipCreated);
    websocketService.on('relationship-deleted', handleRelationshipDeleted);

    return () => {
      websocketService.off('table-created', handleTableCreated);
      websocketService.off('table-updated', handleTableUpdated);
      websocketService.off('table-moved', handleTableMoved);
      websocketService.off('table-deleted', handleTableDeleted);
      websocketService.off('column-created', handleColumnCreated);
      websocketService.off('column-updated', handleColumnUpdated);
      websocketService.off('column-deleted', handleColumnDeleted);
      websocketService.off('relationship-created', handleRelationshipCreated);
      websocketService.off('relationship-deleted', handleRelationshipDeleted);
    };
  }, [diagramId, user?.id]);
}
```

---

### 6. API Service

```typescript
// src/services/api.service.ts
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

class ApiService {
  private client: AxiosInstance;
  private refreshPromise: Promise<string> | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor - add auth token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor - handle token refresh
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
        
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const newToken = await this.refreshToken();
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return this.client(originalRequest);
          } catch (refreshError) {
            // Refresh failed - logout
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return Promise.reject(refreshError);
          }
        }
        
        return Promise.reject(error);
      }
    );
  }

  private async refreshToken(): Promise<string> {
    // Prevent multiple simultaneous refresh calls
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = (async () => {
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
        refreshToken,
      });
      
      const { accessToken } = response.data;
      localStorage.setItem('accessToken', accessToken);
      
      return accessToken;
    })();

    try {
      return await this.refreshPromise;
    } finally {
      this.refreshPromise = null;
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // AUTH
  // ═══════════════════════════════════════════════════════════════

  async login(email: string, password: string) {
    const response = await this.client.post('/auth/login', { email, password });
    return response.data;
  }

  async register(email: string, password: string, displayName: string) {
    const response = await this.client.post('/auth/register', {
      email,
      password,
      displayName,
    });
    return response.data;
  }

  async logout() {
    await this.client.post('/auth/logout');
  }

  // ═══════════════════════════════════════════════════════════════
  // DIAGRAMS
  // ═══════════════════════════════════════════════════════════════

  async getDiagrams(page = 0, size = 20) {
    const response = await this.client.get('/diagrams', {
      params: { page, size },
    });
    return response.data;
  }

  async getDiagram(id: string) {
    const response = await this.client.get(`/diagrams/${id}`);
    return response.data;
  }

  async createDiagram(data: any) {
    const response = await this.client.post('/diagrams', data);
    return response.data;
  }

  async updateDiagram(id: string, data: any) {
    const response = await this.client.put(`/diagrams/${id}`, data);
    return response.data;
  }

  async deleteDiagram(id: string) {
    await this.client.delete(`/diagrams/${id}`);
  }

  // ═══════════════════════════════════════════════════════════════
  // TABLES
  // ═══════════════════════════════════════════════════════════════

  async createTable(diagramId: string, data: any) {
    const response = await this.client.post(
      `/diagrams/${diagramId}/tables`,
      data
    );
    return response.data;
  }

  async updateTable(tableId: string, data: any) {
    const response = await this.client.put(`/tables/${tableId}`, data);
    return response.data;
  }

  async deleteTable(tableId: string) {
    await this.client.delete(`/tables/${tableId}`);
  }

  async updateTablePosition(tableId: string, x: number, y: number) {
    const response = await this.client.patch(`/tables/${tableId}/position`, {
      positionX: x,
      positionY: y,
    });
    return response.data;
  }

  // ═══════════════════════════════════════════════════════════════
  // COLUMNS
  // ═══════════════════════════════════════════════════════════════

  async createColumn(tableId: string, data: any) {
    const response = await this.client.post(`/tables/${tableId}/columns`, data);
    return response.data;
  }

  async updateColumn(columnId: string, data: any) {
    const response = await this.client.put(`/columns/${columnId}`, data);
    return response.data;
  }

  async deleteColumn(columnId: string) {
    await this.client.delete(`/columns/${columnId}`);
  }

  // ═══════════════════════════════════════════════════════════════
  // RELATIONSHIPS
  // ═══════════════════════════════════════════════════════════════

  async createRelationship(diagramId: string, data: any) {
    const response = await this.client.post(
      `/diagrams/${diagramId}/relationships`,
      data
    );
    return response.data;
  }

  async deleteRelationship(relationshipId: string) {
    await this.client.delete(`/relationships/${relationshipId}`);
  }
}

export const apiService = new ApiService();
```

---

**← Previous:** `11-DTOS-MAPPERS.md` | **Next:** `13-SECURITY-JWT.md` →
