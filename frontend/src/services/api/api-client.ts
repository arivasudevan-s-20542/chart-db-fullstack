import axios, { type AxiosInstance, type AxiosError } from 'axios';
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './token-storage';

// API Base URL - can be configured via environment variable
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Create axios instance
export const apiClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 30000,
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor to handle token refresh
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (value: unknown) => void;
    reject: (error: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
    failedQueue.forEach((promise) => {
        if (error) {
            promise.reject(error);
        } else {
            promise.resolve(token);
        }
    });
    failedQueue = [];
};

apiClient.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as typeof error.config & { _retry?: boolean };

        // If error is 401 and we have a refresh token, try to refresh
        if (error.response?.status === 401 && !originalRequest._retry) {
            const refreshToken = getRefreshToken();

            if (!refreshToken) {
                // No refresh token, logout user
                clearTokens();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            if (isRefreshing) {
                // If already refreshing, queue the request
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then((token) => {
                    if (originalRequest) {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return apiClient(originalRequest);
                    }
                    return Promise.reject(error);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                // Try to refresh the token
                const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                    refreshToken,
                });

                // Backend wraps response in {success, data: {accessToken, refreshToken}}
                const responseData = response.data.data || response.data;
                const { accessToken, refreshToken: newRefreshToken } = responseData;
                setTokens(accessToken, newRefreshToken);
                processQueue(null, accessToken);

                // Retry original request
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return apiClient(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError as Error, null);
                clearTokens();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

// Non-authenticated client for login/register
export const publicApiClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 30000,
});

export default apiClient;
