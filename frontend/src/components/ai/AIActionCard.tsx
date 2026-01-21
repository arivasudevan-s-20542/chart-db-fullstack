import React from 'react';
import { CheckCircle2, XCircle, Table, Columns, Link } from 'lucide-react';
import type { ActionResult } from '@/services/ai-action-executor';

interface AIActionCardProps {
    result: ActionResult;
    onFocus?: (elementId: string, elementType: 'table' | 'column' | 'relationship') => void;
}

/**
 * Clickable action summary card (Copilot-style)
 * Shows what the AI did and lets you click to focus the element
 */
export const AIActionCard: React.FC<AIActionCardProps> = ({ result, onFocus }) => {
    const { success, summary, elementId, elementType } = result;

    const handleClick = () => {
        if (elementId && elementType && onFocus) {
            onFocus(elementId, elementType);
        }
    };

    const getIcon = () => {
        if (!success) return <XCircle className="h-4 w-4 text-red-500" />;
        
        switch (elementType) {
            case 'table':
                return <Table className="h-4 w-4 text-blue-500" />;
            case 'column':
                return <Columns className="h-4 w-4 text-purple-500" />;
            case 'relationship':
                return <Link className="h-4 w-4 text-green-500" />;
            default:
                return <CheckCircle2 className="h-4 w-4 text-green-500" />;
        }
    };

    const canFocus = success && elementId && elementType;

    return (
        <div
            className={`
                group relative flex items-start gap-3 rounded-lg border p-3 transition-all
                ${success ? 'border-green-200 bg-green-50 dark:border-green-900/50 dark:bg-green-950/20' : 'border-red-200 bg-red-50 dark:border-red-900/50 dark:bg-red-950/20'}
                ${canFocus ? 'cursor-pointer hover:shadow-md hover:scale-[1.01]' : ''}
            `}
            onClick={canFocus ? handleClick : undefined}
            role={canFocus ? 'button' : undefined}
            tabIndex={canFocus ? 0 : undefined}
            onKeyDown={
                canFocus
                    ? (e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                              e.preventDefault();
                              handleClick();
                          }
                      }
                    : undefined
            }
        >
            {/* Icon */}
            <div className="flex-shrink-0 mt-0.5">{getIcon()}</div>

            {/* Content */}
            <div className="flex-1 min-w-0">
                <p
                    className={`text-sm font-medium ${
                        success
                            ? 'text-green-900 dark:text-green-100'
                            : 'text-red-900 dark:text-red-100'
                    }`}
                >
                    {summary}
                </p>

                {result.error && (
                    <p className="mt-1 text-xs text-red-700 dark:text-red-300">
                        {result.error}
                    </p>
                )}

                {canFocus && (
                    <p className="mt-1 text-xs text-green-600 dark:text-green-400 opacity-0 group-hover:opacity-100 transition-opacity">
                        Click to focus element →
                    </p>
                )}
            </div>

            {/* Decorative border on hover */}
            {canFocus && (
                <div className="absolute inset-0 rounded-lg border-2 border-blue-400 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
            )}
        </div>
    );
};
