import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/auth-context';
import { Spinner } from '@/components/spinner/spinner';

interface ProtectedRouteProps {
    children: React.ReactNode;
}

/**
 * ProtectedRoute component that redirects to login if user is not authenticated.
 * Shows loading spinner while checking auth status.
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    // Show loading while checking auth
    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-background">
                <Spinner size="large" />
            </div>
        );
    }

    // Redirect to login if not authenticated
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return <>{children}</>;
};

/**
 * GuestRoute component that redirects to home if user is already authenticated.
 * Used for login/register pages.
 */
export const GuestRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();

    // Show loading while checking auth
    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-background">
                <Spinner size="large" />
            </div>
        );
    }

    // Redirect to home if already authenticated
    if (isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
};

export default ProtectedRoute;
