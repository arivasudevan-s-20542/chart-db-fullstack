const ACCESS_TOKEN_KEY = 'chartdb_access_token';
const REFRESH_TOKEN_KEY = 'chartdb_refresh_token';

export const getAccessToken = (): string | null => {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
};

export const getRefreshToken = (): string | null => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
};

export const setTokens = (accessToken: string, refreshToken: string): void => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
};

export const clearTokens = (): void => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
};

export const isAuthenticated = (): boolean => {
    const token = getAccessToken();
    if (!token) return false;

    try {
        // Decode JWT and check expiration
        const payload = JSON.parse(atob(token.split('.')[1]));
        const exp = payload.exp * 1000; // Convert to milliseconds
        return Date.now() < exp;
    } catch {
        return false;
    }
};

export interface TokenPayload {
    sub: string; // user id
    email: string;
    name?: string;
    exp: number;
    iat: number;
}

export const getTokenPayload = (): TokenPayload | null => {
    const token = getAccessToken();
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload as TokenPayload;
    } catch {
        return null;
    }
};
