import { apiClient } from './api-client';

export interface AIProvider {
    code: string;
    name: string;
    models?: string[];
}

export interface AIConfig {
    provider: string;
    apiKey: string;
    model: string;
}

export interface AIConfigResponse {
    config?: AIConfig;
    usageStats?: {
        totalTokens: number;
        totalRequests: number;
        lastUsed: string;
    };
    configured: boolean;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
}

export const aiConfigApi = {
    // Get current AI configuration
    getConfig: async (): Promise<AIConfigResponse> => {
        const response = await apiClient.get<ApiResponse<AIConfigResponse>>(
            '/ai/config'
        );
        return response.data.data;
    },

    // Save AI configuration
    saveConfig: async (config: AIConfig): Promise<void> => {
        await apiClient.post('/ai/config', config);
    },

    // Get available providers
    getProviders: async (): Promise<AIProvider[]> => {
        const response = await apiClient.get<
            ApiResponse<{ providers: AIProvider[] }>
        >('/ai/config/providers');
        return response.data.data.providers;
    },

    // Delete configuration
    deleteConfig: async (): Promise<void> => {
        await apiClient.delete('/ai/config');
    },

    // Validate API key
    validateApiKey: async (
        provider: string,
        apiKey: string
    ): Promise<boolean> => {
        try {
            await apiClient.post('/ai/config/validate', { provider, apiKey });
            return true;
        } catch {
            return false;
        }
    },
};
