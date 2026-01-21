import React, { useEffect } from 'react';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import { Button } from '@/components/button/button';
import { useAIAssistantStore } from '@/stores/ai-assistant.store';
import type { AIChatSession } from '@/types/ai.types';
import { Bot, Plus, MessageSquare, Trash2 } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

interface AISessionListProps {
    diagramId: string;
    onStartNewSession: () => void;
    onSelectSession: (session: AIChatSession) => void;
}

export const AISessionList: React.FC<AISessionListProps> = ({
    diagramId,
    onStartNewSession,
    onSelectSession,
}) => {
    const {
        sessions,
        activeSessionId,
        isLoadingSessions,
        loadSessions,
        endSession,
    } = useAIAssistantStore();

    useEffect(() => {
        loadSessions(diagramId);
    }, [diagramId, loadSessions]);

    const handleDeleteSession = async (
        sessionId: string,
        e: React.MouseEvent
    ) => {
        e.stopPropagation();
        if (confirm('Are you sure you want to delete this conversation?')) {
            await endSession(sessionId);
        }
    };

    if (isLoadingSessions) {
        return (
            <Card>
                <CardHeader>
                    <CardTitle>AI Conversations</CardTitle>
                    <CardDescription>Loading conversations...</CardDescription>
                </CardHeader>
            </Card>
        );
    }

    return (
        <Card>
            <CardHeader>
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle>AI Conversations</CardTitle>
                        <CardDescription>
                            Chat history for this diagram
                        </CardDescription>
                    </div>
                    <Button onClick={onStartNewSession} size="sm">
                        <Plus className="mr-2 size-4" />
                        New Chat
                    </Button>
                </div>
            </CardHeader>
            <CardContent>
                {sessions.length === 0 ? (
                    <div className="py-8 text-center">
                        <Bot className="mx-auto size-12 text-gray-400" />
                        <p className="mt-4 text-sm text-gray-500">
                            No conversations yet
                        </p>
                        <Button
                            onClick={onStartNewSession}
                            variant="outline"
                            size="sm"
                            className="mt-4"
                        >
                            Start Your First Chat
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {sessions.map((session) => (
                            <div
                                key={session.id}
                                onClick={() => onSelectSession(session)}
                                className={`
                                    flex cursor-pointer items-center justify-between rounded-lg border-2 p-3 transition-colors
                                    ${
                                        activeSessionId === session.id
                                            ? 'border-purple-500 bg-purple-50 dark:bg-purple-950'
                                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                                    }
                                `}
                            >
                                <div className="flex flex-1 items-center gap-3">
                                    <MessageSquare className="size-5 text-purple-500" />
                                    <div className="min-w-0 flex-1">
                                        <h4 className="truncate font-medium">
                                            {session.title}
                                        </h4>
                                        <p className="text-xs text-gray-400">
                                            {formatDistanceToNow(
                                                new Date(session.createdAt),
                                                { addSuffix: true }
                                            )}
                                        </p>
                                    </div>
                                </div>

                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={(e) =>
                                        handleDeleteSession(session.id, e)
                                    }
                                    title="Delete conversation"
                                >
                                    <Trash2 className="size-4 text-red-500" />
                                </Button>
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>
        </Card>
    );
};
