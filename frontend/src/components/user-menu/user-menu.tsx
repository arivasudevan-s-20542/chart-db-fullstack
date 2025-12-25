import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/auth-context';
import { Button } from '@/components/button/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/dropdown-menu/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/avatar/avatar';
import { LogOut, Settings, User, UserPlus, LogIn } from 'lucide-react';

export const UserMenu: React.FC = () => {
    const { user, isAuthenticated, logout, isLoading } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    const handleLogin = () => {
        navigate('/login');
    };

    const handleRegister = () => {
        navigate('/register');
    };

    // Show login/register buttons when not authenticated
    if (!isAuthenticated) {
        return (
            <div className="flex items-center gap-2">
                <Button variant="ghost" size="sm" onClick={handleLogin} disabled={isLoading}>
                    <LogIn className="mr-2 size-4" />
                    Sign In
                </Button>
                <Button size="sm" onClick={handleRegister} disabled={isLoading}>
                    <UserPlus className="mr-2 size-4" />
                    Sign Up
                </Button>
            </div>
        );
    }

    // Get user initials for avatar
    const initials = user
        ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || user.email[0].toUpperCase()
        : '?';

    const displayName = user
        ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email
        : 'User';

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative size-8 rounded-full">
                    <Avatar className="size-8">
                        <AvatarFallback className="bg-primary text-primary-foreground">
                            {initials}
                        </AvatarFallback>
                    </Avatar>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-56" align="end" forceMount>
                <DropdownMenuLabel className="font-normal">
                    <div className="flex flex-col space-y-1">
                        <p className="text-sm font-medium leading-none">{displayName}</p>
                        <p className="text-xs leading-none text-muted-foreground">
                            {user?.email}
                        </p>
                    </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => navigate('/profile')}>
                    <User className="mr-2 size-4" />
                    <span>Profile</span>
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => navigate('/settings')}>
                    <Settings className="mr-2 size-4" />
                    <span>Settings</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout} className="text-destructive">
                    <LogOut className="mr-2 size-4" />
                    <span>Log out</span>
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default UserMenu;
