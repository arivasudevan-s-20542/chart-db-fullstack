import { publicApiClient } from './api-client';
import { setTokens, clearTokens } from './token-storage';

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    fullName: string; // Backend expects fullName, not firstName/lastName
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    user?: UserProfile;
}

// Backend wraps responses in ApiResponse
interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

export interface UserProfile {
    id: string;
    email: string;
    firstName?: string;
    lastName?: string;
    username?: string;
    displayName?: string;
    cursorColor?: string;
    preferences?: Record<string, unknown>;
    isActive?: boolean;
    createdAt: string;
    updatedAt: string;
}

export const authApi = {
    /**
     * Login with email and password
     */
    login: async (request: LoginRequest): Promise<AuthResponse> => {
        const response = await publicApiClient.post<ApiResponse<AuthResponse>>(
            '/auth/login',
            request
        );
        const authData = response.data.data; // Extract from ApiResponse wrapper
        const { accessToken, refreshToken } = authData;
        setTokens(accessToken, refreshToken);
        return authData;
    },

    /**
     * Register a new user
     */
    register: async (request: {
        email: string;
        password: string;
        firstName: string;
        lastName: string;
    }): Promise<AuthResponse> => {
        // Convert firstName/lastName to fullName for backend
        const backendRequest: RegisterRequest = {
            email: request.email,
            password: request.password,
            fullName: `${request.firstName} ${request.lastName}`.trim(),
        };
        const response = await publicApiClient.post<ApiResponse<AuthResponse>>(
            '/auth/register',
            backendRequest
        );
        const authData = response.data.data; // Extract from ApiResponse wrapper
        const { accessToken, refreshToken } = authData;
        setTokens(accessToken, refreshToken);
        return authData;
    },

    /**
     * Logout - clear tokens
     */
    logout: (): void => {
        clearTokens();
    },

    /**
     * Refresh access token
     */
    refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
        const response = await publicApiClient.post<ApiResponse<AuthResponse>>(
            '/auth/refresh',
            {
                refreshToken,
            }
        );
        const authData = response.data.data;
        const { accessToken, refreshToken: newRefreshToken } = authData;
        setTokens(accessToken, newRefreshToken);
        return authData;
    },

    /**
     * Get current user profile - uses /auth/me endpoint
     */
    getCurrentUser: async (): Promise<UserProfile> => {
        const { apiClient } = await import('./api-client');
        const response =
            await apiClient.get<ApiResponse<UserProfile>>('/auth/me');
        return response.data.data;
    },

    /**
     * Update user profile
     */
    updateProfile: async (data: Partial<UserProfile>): Promise<UserProfile> => {
        const { apiClient } = await import('./api-client');
        const response = await apiClient.put<ApiResponse<UserProfile>>(
            '/users/me',
            data
        );
        return response.data.data;
    },

    /**
     * Change password
     */
    changePassword: async (
        currentPassword: string,
        newPassword: string
    ): Promise<void> => {
        const { apiClient } = await import('./api-client');
        await apiClient.post('/users/me/change-password', {
            currentPassword,
            newPassword,
        });
    },
};

export default authApi;
