import React, { useCallback, useEffect, useState } from 'react';
import { authContext, type AuthContextValue } from './auth-context';
import {
    authApi,
    isAuthenticated as checkIsAuthenticated,
    getAccessToken,
    clearTokens,
    setTokens,
    type UserProfile,
} from '@/services/api';
import { wsService } from '@/services/api/websocket.service';

export interface AuthProviderProps {
    children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<UserProfile | null>(null);
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Initialize auth state on mount
    useEffect(() => {
        const initAuth = async () => {
            setIsLoading(true);
            try {
                if (checkIsAuthenticated() && getAccessToken()) {
                    // Try to fetch user profile
                    const userProfile = await authApi.getCurrentUser();
                    setUser(userProfile);
                    setIsAuthenticated(true);

                    // Connect to WebSocket
                    try {
                        await wsService.connect();
                    } catch (wsError) {
                        console.warn('WebSocket connection failed:', wsError);
                    }
                } else {
                    clearTokens();
                    setUser(null);
                    setIsAuthenticated(false);
                }
            } catch (err) {
                console.error('Auth initialization failed:', err);
                clearTokens();
                setUser(null);
                setIsAuthenticated(false);
            } finally {
                setIsLoading(false);
            }
        };

        initAuth();

        return () => {
            wsService.disconnect();
        };
    }, []);

    const login = useCallback(async (email: string, password: string) => {
        setIsLoading(true);
        setError(null);
        try {
            await authApi.login({ email, password });
            const userProfile = await authApi.getCurrentUser();
            setUser(userProfile);
            setIsAuthenticated(true);

            // Connect to WebSocket
            try {
                await wsService.connect();
            } catch (wsError) {
                console.warn('WebSocket connection failed:', wsError);
            }
        } catch (err: any) {
            const message =
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Login failed. Please check your credentials.';
            setError(message);
            throw new Error(message);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const register = useCallback(
        async (
            email: string,
            password: string,
            firstName: string,
            lastName: string
        ) => {
            setIsLoading(true);
            setError(null);
            try {
                await authApi.register({
                    email,
                    password,
                    firstName,
                    lastName,
                });
                const userProfile = await authApi.getCurrentUser();
                setUser(userProfile);
                setIsAuthenticated(true);

                // Connect to WebSocket
                try {
                    await wsService.connect();
                } catch (wsError) {
                    console.warn('WebSocket connection failed:', wsError);
                }
            } catch (err: any) {
                const message =
                    err.response?.data?.message ||
                    err.response?.data?.error ||
                    'Registration failed. Please try again.';
                setError(message);
                throw new Error(message);
            } finally {
                setIsLoading(false);
            }
        },
        []
    );

    const logout = useCallback(() => {
        wsService.disconnect();
        authApi.logout();
        setUser(null);
        setIsAuthenticated(false);
        setError(null);
    }, []);

    const refreshUser = useCallback(async () => {
        try {
            const userProfile = await authApi.getCurrentUser();
            setUser(userProfile);
        } catch (err) {
            console.error('Failed to refresh user:', err);
        }
    }, []);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    /**
     * Set auth tokens directly (used for OAuth2 callback)
     */
    const setAuthTokens = useCallback(async (accessToken: string, refreshToken: string) => {
        setIsLoading(true);
        setError(null);
        try {
            // Store tokens
            setTokens(accessToken, refreshToken);
            
            // Fetch user profile
            const userProfile = await authApi.getCurrentUser();
            setUser(userProfile);
            setIsAuthenticated(true);

            // Connect to WebSocket
            try {
                await wsService.connect();
            } catch (wsError) {
                console.warn('WebSocket connection failed:', wsError);
            }
        } catch (err: any) {
            clearTokens();
            setUser(null);
            setIsAuthenticated(false);
            const message = err.response?.data?.message || err.message || 'Authentication failed';
            setError(message);
            throw new Error(message);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const value: AuthContextValue = {
        user,
        isAuthenticated,
        isLoading,
        error,
        login,
        register,
        logout,
        refreshUser,
        clearError,
        setAuthTokens,
    };

    return (
        <authContext.Provider value={value}>{children}</authContext.Provider>
    );
};

export default AuthProvider;
