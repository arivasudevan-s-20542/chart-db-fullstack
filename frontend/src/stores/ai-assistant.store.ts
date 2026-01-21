import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import {
    AIChatSession,
    AIMessage,
    StartChatSessionRequest,
    ChatMessageRequest,
    UserAIConfig,
    AIAgent,
} from '@/types/ai.types';
import { aiApi } from '@/services/api/ai.api';

interface AIAssistantState {
    // Sessions
    sessions: AIChatSession[];
    activeSessionId: string | null;
    isLoadingSessions: boolean;

    // Messages
    messages: AIMessage[];
    isLoadingMessages: boolean;
    isSendingMessage: boolean;
    messageError: string | null;

    // Config
    userConfig: UserAIConfig | null;
    isLoadingConfig: boolean;

    // Agents
    availableAgents: AIAgent[];
    isLoadingAgents: boolean;

    // Actions
    loadSessions: (diagramId: string) => Promise<void>;
    loadConfig: () => Promise<void>;
    updateConfig: (config: Partial<UserAIConfig>) => Promise<void>;
    startSession: (request: StartChatSessionRequest) => Promise<AIChatSession>;
    setActiveSession: (sessionId: string | null) => void;
    loadMessages: (sessionId: string) => Promise<void>;
    sendMessage: (sessionId: string, request: ChatMessageRequest) => Promise<void>;
    endSession: (sessionId: string) => Promise<void>;
    clearError: () => void;
}

export const useAIAssistantStore = create<AIAssistantState>()(
    devtools(
        (set, get) => ({
            // Initial state
            sessions: [],
            activeSessionId: null,
            isLoadingSessions: false,
            messages: [],
            isLoadingMessages: false,
            isSendingMessage: false,
            messageError: null,
            userConfig: null,
            isLoadingConfig: false,
            availableAgents: [],
            isLoadingAgents: false,

            // Load sessions for a diagram
            loadSessions: async (diagramId: string) => {
                set({ isLoadingSessions: true });
                try {
                    const sessions = await aiApi.getActiveSessions(diagramId);
                    set({ sessions, isLoadingSessions: false });
                } catch (error) {
                    set({ isLoadingSessions: false });
                }
            },

            // Load user AI configuration
            loadConfig: async () => {
                set({ isLoadingConfig: true });
                try {
                    const config = await aiApi.getCurrentConfig();
                    set({ userConfig: config, isLoadingConfig: false });
                } catch (error) {
                    console.error('Failed to load AI config:', error);
                    set({ isLoadingConfig: false });
                }
            },

            // Update user AI configuration
            updateConfig: async (configUpdate: Partial<UserAIConfig>) => {
                try {
                    const currentConfig = get().userConfig || {
                        defaultProvider: 'mistral',
                        defaultModel: 'mistral-small-latest',
                        temperature: 0.7,
                    };
                    
                    const updatedConfig = { ...currentConfig, ...configUpdate };
                    await aiApi.updateConfig(updatedConfig);
                    set({ userConfig: updatedConfig });
                } catch (error) {
                    console.error('Failed to update AI config:', error);
                    throw error;
                }
            },

            // Start a new chat session
            startSession: async (request: StartChatSessionRequest) => {
                set({ messageError: null });
                try {
                    const session = await aiApi.startChatSession(request);
                    set((state) => ({
                        sessions: [session, ...state.sessions],
                        activeSessionId: session.id,
                        messages: [],
                    }));
                    return session;
                } catch (error) {
                    const message = error instanceof Error ? error.message : 'Failed to start chat session';
                    set({ messageError: message });
                    throw error;
                }
            },

            // Set active session
            setActiveSession: (sessionId: string | null) => {
                set({ activeSessionId: sessionId, messages: [], messageError: null });
                if (sessionId) {
                    get().loadMessages(sessionId);
                }
            },

            // Load messages for a session
            loadMessages: async (sessionId: string) => {
                set({ isLoadingMessages: true, messageError: null });
                try {
                    const messages = await aiApi.getChatHistory(sessionId);
                    set({ messages, isLoadingMessages: false });
                } catch (error) {
                    set({
                        messageError: error instanceof Error ? error.message : 'Failed to load messages',
                        isLoadingMessages: false,
                    });
                }
            },

            // Send a message
            sendMessage: async (sessionId: string, request: ChatMessageRequest) => {
                set({ isSendingMessage: true, messageError: null });
                
                // Add user message immediately
                const userMessage: AIMessage = {
                    id: `user-${Date.now()}`,
                    sessionId,
                    role: 'user',
                    content: request.content,
                    createdAt: new Date().toISOString(),
                };
                
                // Add assistant message placeholder for streaming
                const assistantMessageId = `assistant-${Date.now()}`;
                const assistantMessage: AIMessage = {
                    id: assistantMessageId,
                    sessionId,
                    role: 'assistant',
                    content: '',
                    createdAt: new Date().toISOString(),
                };
                
                set((state) => ({
                    messages: [...state.messages, userMessage, assistantMessage],
                }));

                try {
                    await aiApi.sendMessage(sessionId, request, (chunk: string) => {
                        // Update the assistant message with streaming content
                        set((state) => ({
                            messages: state.messages.map((msg) =>
                                msg.id === assistantMessageId
                                    ? { ...msg, content: msg.content + chunk }
                                    : msg
                            ),
                        }));
                    });
                    
                    set({ isSendingMessage: false });
                } catch (error) {
                    // Remove the placeholder assistant message on error
                    set((state) => ({
                        messages: state.messages.filter((msg) => msg.id !== assistantMessageId),
                        messageError: error instanceof Error ? error.message : 'Failed to send message',
                        isSendingMessage: false,
                    }));
                }
            },

            // End a session
            endSession: async (sessionId: string) => {
                try {
                    await aiApi.endSession(sessionId);
                    set((state) => ({
                        sessions: state.sessions.filter((s) => s.id !== sessionId),
                        activeSessionId: state.activeSessionId === sessionId ? null : state.activeSessionId,
                        messages: state.activeSessionId === sessionId ? [] : state.messages,
                    }));
                } catch (error) {
                    throw error;
                }
            },

            // Clear errors
            clearError: () => {
                set({ messageError: null });
            },
        }),
        { name: 'AIAssistantStore' }
    )
);
