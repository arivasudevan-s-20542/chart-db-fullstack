import React from 'react';
import { useAuth } from '@/context/auth-context';
import { StorageProvider } from './storage-provider';
import { BackendStorageProvider } from './backend-storage-provider';

/**
 * HybridStorageProvider automatically switches between local storage (IndexedDB)
 * and backend storage based on authentication status.
 *
 * - When authenticated: Uses BackendStorageProvider (API calls to backend)
 * - When not authenticated: Uses StorageProvider (IndexedDB/localStorage)
 */
export const HybridStorageProvider: React.FC<React.PropsWithChildren> = ({
    children,
}) => {
    const { isAuthenticated, isLoading } = useAuth();

    console.log(
        '[HybridStorageProvider] isAuthenticated:',
        isAuthenticated,
        'isLoading:',
        isLoading
    );

    // Show loading state while checking auth
    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <div className="animate-pulse text-muted-foreground">
                    Loading...
                </div>
            </div>
        );
    }

    // Use backend storage when authenticated, local storage otherwise
    if (isAuthenticated) {
        console.log('[HybridStorageProvider] Using BackendStorageProvider');
        return <BackendStorageProvider>{children}</BackendStorageProvider>;
    }

    console.log('[HybridStorageProvider] Using StorageProvider (IndexedDB)');
    return <StorageProvider>{children}</StorageProvider>;
};

export default HybridStorageProvider;
