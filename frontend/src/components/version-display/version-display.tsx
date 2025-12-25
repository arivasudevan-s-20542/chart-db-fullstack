import React from 'react';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/tooltip/tooltip';

// Build-time version info injected by Vite
const BUILD_VERSION = import.meta.env.VITE_BUILD_VERSION || 'dev';
const BUILD_COMMIT = import.meta.env.VITE_BUILD_COMMIT || 'local';
const BUILD_TIME = import.meta.env.VITE_BUILD_TIME || new Date().toISOString();

export const VersionDisplay: React.FC = () => {
    const shortCommit = BUILD_COMMIT.substring(0, 7);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className="fixed bottom-2 right-2 cursor-help rounded bg-muted/50 px-2 py-1 text-xs text-muted-foreground opacity-50 hover:opacity-100">
                    v{BUILD_VERSION} ({shortCommit})
                </div>
            </TooltipTrigger>
            <TooltipContent side="left">
                <div className="space-y-1 text-xs">
                    <div>
                        <span className="font-medium">Version:</span>{' '}
                        {BUILD_VERSION}
                    </div>
                    <div>
                        <span className="font-medium">Commit:</span>{' '}
                        {BUILD_COMMIT}
                    </div>
                    <div>
                        <span className="font-medium">Built:</span>{' '}
                        {new Date(BUILD_TIME).toLocaleString()}
                    </div>
                </div>
            </TooltipContent>
        </Tooltip>
    );
};

export default VersionDisplay;
