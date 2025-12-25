import React, { useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/context/auth-context';
import { Button } from '@/components/button/button';
import { Input } from '@/components/input/input';
import { Label } from '@/components/label/label';
import { Spinner } from '@/components/spinner/spinner';
import { DiagramIcon } from '@/components/diagram-icon/diagram-icon';

export const RegisterPage: React.FC = () => {
    const navigate = useNavigate();
    const { register, isLoading, error, clearError } = useAuth();
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [localError, setLocalError] = useState<string | null>(null);

    const handleSubmit = useCallback(
        async (e: React.FormEvent) => {
            e.preventDefault();
            setLocalError(null);
            clearError();

            if (!firstName || !lastName || !email || !password || !confirmPassword) {
                setLocalError('Please fill in all fields');
                return;
            }

            if (password !== confirmPassword) {
                setLocalError('Passwords do not match');
                return;
            }

            if (password.length < 8) {
                setLocalError('Password must be at least 8 characters');
                return;
            }

            try {
                await register(email, password, firstName, lastName);
                navigate('/', { replace: true });
            } catch (err: any) {
                setLocalError(err.message || 'Registration failed');
            }
        },
        [firstName, lastName, email, password, confirmPassword, register, navigate, clearError]
    );

    return (
        <div className="flex min-h-screen items-center justify-center bg-background">
            <div className="w-full max-w-md space-y-8 rounded-lg border bg-card p-8 shadow-lg">
                <div className="flex flex-col items-center space-y-2">
                    <DiagramIcon />
                    <h1 className="text-2xl font-bold tracking-tight">Create Account</h1>
                    <p className="text-sm text-muted-foreground">
                        Sign up to start creating diagrams
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {(error || localError) && (
                        <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                            {error || localError}
                        </div>
                    )}

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="firstName">First Name</Label>
                            <Input
                                id="firstName"
                                type="text"
                                placeholder="John"
                                value={firstName}
                                onChange={(e) => setFirstName(e.target.value)}
                                disabled={isLoading}
                                autoComplete="given-name"
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="lastName">Last Name</Label>
                            <Input
                                id="lastName"
                                type="text"
                                placeholder="Doe"
                                value={lastName}
                                onChange={(e) => setLastName(e.target.value)}
                                disabled={isLoading}
                                autoComplete="family-name"
                                required
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="email">Email</Label>
                        <Input
                            id="email"
                            type="email"
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={isLoading}
                            autoComplete="email"
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="password">Password</Label>
                        <Input
                            id="password"
                            type="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            disabled={isLoading}
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="confirmPassword">Confirm Password</Label>
                        <Input
                            id="confirmPassword"
                            type="password"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            disabled={isLoading}
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? (
                            <>
                                <Spinner className="mr-2 size-4" />
                                Creating account...
                            </>
                        ) : (
                            'Create Account'
                        )}
                    </Button>
                </form>

                <div className="text-center text-sm">
                    Already have an account?{' '}
                    <Link
                        to="/login"
                        className="font-medium text-primary underline-offset-4 hover:underline"
                    >
                        Sign in
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
