import { apiClient } from './api-client';

export interface McpApiToken {
    id: string;
    name: string;
    tokenPrefix: string;
    scopes: string;
    lastUsedAt: string | null;
    expiresAt: string | null;
    active: boolean;
    createdAt: string;
    /** Plain-text token — only present when a token is first created */
    token?: string;
}

export interface CreateMcpApiTokenRequest {
    name: string;
    scopes?: string;
    expiresAt?: string;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
}

export const mcpTokenApi = {
    /** Create a new MCP API token */
    createToken: async (
        request: CreateMcpApiTokenRequest
    ): Promise<McpApiToken> => {
        const response = await apiClient.post<ApiResponse<McpApiToken>>(
            '/mcp/tokens',
            request
        );
        return response.data.data;
    },

    /** List all MCP API tokens for the current user */
    listTokens: async (): Promise<McpApiToken[]> => {
        const response =
            await apiClient.get<ApiResponse<McpApiToken[]>>('/mcp/tokens');
        return response.data.data;
    },

    /** Revoke a specific MCP API token */
    revokeToken: async (tokenId: string): Promise<void> => {
        await apiClient.delete(`/mcp/tokens/${tokenId}`);
    },

    /** Revoke all MCP API tokens */
    revokeAllTokens: async (): Promise<void> => {
        await apiClient.delete('/mcp/tokens');
    },
};
