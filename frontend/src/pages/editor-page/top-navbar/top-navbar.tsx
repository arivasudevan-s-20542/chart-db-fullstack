import React, { useCallback } from 'react';
import ChartDBLogo from '@/assets/logo-light.png';
import ChartDBDarkLogo from '@/assets/logo-dark.png';
import { useTheme } from '@/hooks/use-theme';
import { DiagramName } from './diagram-name';
import { LastSaved } from './last-saved';
import { LanguageNav } from './language-nav/language-nav';
import { Menu } from './menu/menu';
import { UserMenu } from '@/components/user-menu';
import { CollaborationIndicator } from '@/components/collaboration-indicator';
import { Button } from '@/components/button/button';
import { Share2 } from 'lucide-react';
import { useDialog } from '@/hooks/use-dialog';
import { useAuth } from '@/context/auth-context';
import { useChartDB } from '@/hooks/use-chartdb';
import {
    Tooltip,
    TooltipTrigger,
    TooltipContent,
} from '@/components/tooltip/tooltip';

export interface TopNavbarProps {}

export const TopNavbar: React.FC<TopNavbarProps> = () => {
    const { effectiveTheme } = useTheme();
    const { openShareDiagramDialog } = useDialog();
    const { isAuthenticated } = useAuth();
    const { permissionLevel } = useChartDB();

    // Only OWNER can share diagrams
    const canShare = permissionLevel === 'OWNER';

    const renderStars = useCallback(() => {
        return (
            <iframe
                src={`https://ghbtns.com/github-btn.html?user=chartdb&repo=chartdb&type=star&size=large&text=false`}
                width="40"
                height="30"
                title="GitHub"
            ></iframe>
        );
    }, []);

    return (
        <nav className="flex flex-col justify-between border-b px-3 md:h-12 md:flex-row md:items-center md:px-4">
            <div className="flex flex-1 flex-col justify-between gap-x-1 md:flex-row md:justify-normal">
                <div className="flex items-center justify-between pt-[8px] font-primary md:py-[10px]">
                    <a
                        href="https://chartdb.io"
                        className="cursor-pointer"
                        rel="noreferrer"
                    >
                        <img
                            src={
                                effectiveTheme === 'light'
                                    ? ChartDBLogo
                                    : ChartDBDarkLogo
                            }
                            alt="chartDB"
                            className="h-4 max-w-fit"
                        />
                    </a>
                </div>
                <Menu />
            </div>
            <DiagramName />
            <div className="hidden flex-1 items-center justify-end gap-2 sm:flex">
                <CollaborationIndicator />
                {isAuthenticated && canShare && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghost"
                                size="sm"
                                className="gap-2"
                                onClick={() => openShareDiagramDialog()}
                            >
                                <Share2 className="size-4" />
                                <span className="hidden lg:inline">Share</span>
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>Share diagram</TooltipContent>
                    </Tooltip>
                )}
                <LastSaved />
                {renderStars()}
                <LanguageNav />
                <UserMenu />
            </div>
        </nav>
    );
};
