import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import { TooltipProvider } from './components/tooltip/tooltip';
import { HelmetData } from './helmet/helmet-data';
import { HelmetProvider } from 'react-helmet-async';
import { AuthProvider } from './context/auth-context';
import { CollaborationProvider } from './context/collaboration-context';

export const App = () => {
    return (
        <HelmetProvider>
            <HelmetData />
            <AuthProvider>
                <CollaborationProvider>
                    <TooltipProvider>
                        <RouterProvider router={router} />
                    </TooltipProvider>
                </CollaborationProvider>
            </AuthProvider>
        </HelmetProvider>
    );
};
