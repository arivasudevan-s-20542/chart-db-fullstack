import { createContext } from 'react';
import type { UserProfile } from '@/services/api';

export interface AuthContextValue {
    // State
    user: UserProfile | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    // Actions
    login: (email: string, password: string) => Promise<void>;
    register: (
        email: string,
        password: string,
        firstName: string,
        lastName: string
    ) => Promise<void>;
    logout: () => void;
    refreshUser: () => Promise<void>;
    clearError: () => void;
    // OAuth2 token handler
    setAuthTokens: (accessToken: string, refreshToken: string) => Promise<void>;
}

export const authContext = createContext<AuthContextValue>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
    login: async () => {},
    register: async () => {},
    logout: () => {},
    refreshUser: async () => {},
    clearError: () => {},
    setAuthTokens: async () => {},
});

export default authContext;
