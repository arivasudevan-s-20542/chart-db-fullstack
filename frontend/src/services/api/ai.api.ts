import apiClient from './api-client';
import { getAccessToken } from './token-storage';
import type {
    AIChatSession,
    AIMessage,
    StartChatSessionRequest,
    ChatMessageRequest,
    UserAIConfig,
} from '@/types/ai.types';

export type { AIChatSession, AIMessage, StartChatSessionRequest, ChatMessageRequest, UserAIConfig };

export const aiApi = {
    // Chat Session Management
    startChatSession: async (request: StartChatSessionRequest): Promise<AIChatSession> => {
        const response = await apiClient.post('/ai/chat/sessions', request);
        return response.data.data;
    },

    getActiveSessions: async (diagramId: string): Promise<AIChatSession[]> => {
        const response = await apiClient.get(`/ai/chat/sessions/diagram/${diagramId}`);
        return response.data.data;
    },

    sendMessage: async (
        sessionId: string, 
        request: ChatMessageRequest,
        onChunk?: (chunk: string) => void
    ): Promise<AIMessage> => {
        // If no streaming callback provided, use regular POST
        if (!onChunk) {
            const response = await apiClient.post(`/ai/chat/sessions/${sessionId}/messages`, { 
                message: request.content,
                includeContext: request.includeContext 
            });
            return response.data.data;
        }

        // Use fetch for streaming
        const baseURL = apiClient.defaults.baseURL || '';
        const token = getAccessToken();
        
        const response = await fetch(`${baseURL}/ai/chat/sessions/${sessionId}/messages`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token && { 'Authorization': `Bearer ${token}` }),
            },
            body: JSON.stringify({
                message: request.content,
                includeContext: request.includeContext,
            }),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Check if response is streaming (Server-Sent Events)
        const contentType = response.headers.get('content-type');
        if (contentType?.includes('text/event-stream')) {
            // Handle SSE streaming
            const reader = response.body?.getReader();
            const decoder = new TextDecoder();
            let fullContent = '';

            if (reader) {
                try {
                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;

                        const chunk = decoder.decode(value, { stream: true });
                        const lines = chunk.split('\n');

                        for (const line of lines) {
                            if (line.startsWith('data: ')) {
                                const data = line.slice(6);
                                if (data === '[DONE]') continue;
                                
                                try {
                                    const parsed = JSON.parse(data);
                                    if (parsed.content) {
                                        fullContent += parsed.content;
                                        onChunk(parsed.content);
                                    }
                                } catch (e) {
                                    // Skip invalid JSON
                                }
                            }
                        }
                    }
                } finally {
                    reader.releaseLock();
                }
            }

            // Return the complete message
            return {
                id: Date.now().toString(),
                sessionId,
                role: 'assistant',
                content: fullContent,
                createdAt: new Date().toISOString(),
            } as AIMessage;
        } else {
            // Non-streaming response
            const data = await response.json();
            return data.data;
        }
    },

    getChatHistory: async (sessionId: string): Promise<AIMessage[]> => {
        const response = await apiClient.get(`/ai/chat/sessions/${sessionId}/history`);
        return response.data.data;
    },

    endSession: async (sessionId: string): Promise<void> => {
        await apiClient.delete(`/ai/chat/sessions/${sessionId}`);
    },

    // User AI Configuration
    getCurrentConfig: async (): Promise<UserAIConfig> => {
        const response = await apiClient.get('/ai/config');
        const data = response.data.data;
        
        // Transform the API response to match UserAIConfig interface
        if (!data.configured || !data.config) {
            throw new Error('AI configuration not found. Please configure your AI provider settings.');
        }
        
        return {
            id: '', // Not returned by API
            userId: '', // Not returned by API
            defaultProvider: data.config.provider || 'openai',
            defaultModel: data.config.model || '',
            maxTokens: 2000,
            temperature: 0.7,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
        };
    },
};
