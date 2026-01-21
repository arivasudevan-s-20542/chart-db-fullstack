/**
 * AI Assistant Types
 */

export enum MessageRole {
    USER = 'USER',
    ASSISTANT = 'ASSISTANT',
    SYSTEM = 'SYSTEM',
}

export enum ChangeStatus {
    PENDING = 'PENDING',
    APPLIED = 'APPLIED',
    REJECTED = 'REJECTED',
}

export interface AIChatSession {
    id: string;
    diagramId: string;
    title: string;
    agentConfig?: string;
    createdAt: string;
    updatedAt: string;
}

export interface AIMessage {
    id: string;
    sessionId: string;
    role: MessageRole;
    content: string;
    suggestedChanges?: AISuggestedChange[];
    createdAt: string;
}

export interface AISuggestedChange {
    id: string;
    messageId: string;
    changeType: string;
    targetTable?: string;
    changeDetails: string;
    status: ChangeStatus;
    appliedAt?: string;
}

export interface StartChatSessionRequest {
    diagramId: string;
    agentName?: string;
    systemPrompt?: string;
}

export interface ChatMessageRequest {
    content: string;
    includeContext?: boolean;
}

export interface UserAIConfig {
    id: string;
    userId: string;
    openaiApiKey?: string;
    anthropicApiKey?: string;
    defaultProvider: string;
    defaultModel: string;
    maxTokens: number;
    temperature: number;
    createdAt: string;
    updatedAt: string;
}

export interface AIAgent {
    id: string;
    name: string;
    description: string;
    systemPrompt: string;
    capabilities: string[];
    isSystem: boolean;
    createdBy?: string;
}
