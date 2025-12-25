import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context/auth-context';
import { Spinner } from '@/components/spinner/spinner';
import { DatabaseType } from '@/lib/domain/database-type';
import { DiagramIcon } from '@/components/diagram-icon/diagram-icon';

export const OAuth2CallbackPage: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { setAuthTokens } = useAuth();
    const [error, setError] = useState<string | null>(null);
    const [processing, setProcessing] = useState(true);

    useEffect(() => {
        const processOAuth2Callback = async () => {
            try {
                const accessToken = searchParams.get('access_token');
                const refreshToken = searchParams.get('refresh_token');
                const errorMessage = searchParams.get('error');

                if (errorMessage) {
                    setError(decodeURIComponent(errorMessage));
                    setProcessing(false);
                    return;
                }

                if (!accessToken || !refreshToken) {
                    setError('Missing authentication tokens');
                    setProcessing(false);
                    return;
                }

                // Decode tokens and set auth state
                await setAuthTokens(accessToken, refreshToken);

                // Navigate to home page
                navigate('/', { replace: true });
            } catch (err: any) {
                setError(err.message || 'Failed to process authentication');
                setProcessing(false);
            }
        };

        processOAuth2Callback();
    }, [searchParams, setAuthTokens, navigate]);

    if (error) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-background">
                <div className="w-full max-w-md space-y-8 rounded-lg border bg-card p-8 shadow-lg">
                    <div className="flex flex-col items-center space-y-2">
                        <DiagramIcon databaseType={DatabaseType.GENERIC} />
                        <h1 className="text-2xl font-bold tracking-tight text-destructive">
                            Authentication Failed
                        </h1>
                    </div>
                    <div className="rounded-md bg-destructive/10 p-4 text-sm text-destructive">
                        {error}
                    </div>
                    <div className="flex flex-col gap-2">
                        <button
                            onClick={() => navigate('/login')}
                            className="w-full rounded-md bg-primary px-4 py-2 text-primary-foreground hover:bg-primary/90"
                        >
                            Back to Login
                        </button>
                        <button
                            onClick={() => navigate('/')}
                            className="w-full rounded-md border px-4 py-2 hover:bg-accent"
                        >
                            Continue as Guest
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-background">
            <div className="flex flex-col items-center space-y-4">
                <DiagramIcon databaseType={DatabaseType.GENERIC} />
                <Spinner className="size-8" />
                <p className="text-muted-foreground">
                    {processing ? 'Completing sign in...' : 'Redirecting...'}
                </p>
            </div>
        </div>
    );
};

export default OAuth2CallbackPage;
