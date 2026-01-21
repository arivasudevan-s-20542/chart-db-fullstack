import React, { useState } from 'react';
import { AIChatPanel } from './AIChatPanel';
import { AISessionList } from './AISessionList';
import { NewSessionModal } from './NewSessionModal';
import { useAIAssistantStore } from '@/stores/ai-assistant.store';
import { AIChatSession } from '@/types/ai.types';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/tabs/tabs';

interface AIAssistantPanelProps {
    diagramId: string;
    onHighlightTable?: (tableId: string) => void;
}

export const AIAssistantPanel: React.FC<AIAssistantPanelProps> = ({ diagramId, onHighlightTable }) => {
    const [isNewSessionModalOpen, setIsNewSessionModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState('chat');
    const { sessions, activeSessionId, setActiveSession } = useAIAssistantStore();

    const activeSession = sessions.find((s) => s.id === activeSessionId);

    const handleSelectSession = (session: AIChatSession) => {
        setActiveSession(session.id);
        // Switch to chat tab when a session is selected
        setActiveTab('chat');
    };

    return (
        <div className="h-full flex flex-col">
            <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col">
                <TabsList className="w-full justify-start border-b rounded-none bg-transparent p-0">
                    <TabsTrigger
                        value="chat"
                        className="rounded-none border-b-2 border-transparent data-[state=active]:border-purple-500"
                    >
                        Chat
                    </TabsTrigger>
                    <TabsTrigger
                        value="sessions"
                        className="rounded-none border-b-2 border-transparent data-[state=active]:border-purple-500"
                    >
                        Conversations
                    </TabsTrigger>
                </TabsList>

                <div className="flex-1 overflow-hidden p-4">
                    <TabsContent value="chat" className="mt-0 h-full">
                        {activeSession ? (
                            <AIChatPanel sessionId={activeSession.id} onHighlightTable={onHighlightTable} />
                        ) : (
                            <div className="h-full flex items-center justify-center">
                                <div className="text-center">
                                    <p className="text-gray-500 mb-4">No active conversation</p>
                                    <button
                                        onClick={() => setIsNewSessionModalOpen(true)}
                                        className="text-purple-500 hover:underline"
                                    >
                                        Start a new chat
                                    </button>
                                </div>
                            </div>
                        )}
                    </TabsContent>

                    <TabsContent value="sessions" className="mt-0">
                        <AISessionList
                            diagramId={diagramId}
                            onStartNewSession={() => setIsNewSessionModalOpen(true)}
                            onSelectSession={handleSelectSession}
                        />
                    </TabsContent>
                </div>
            </Tabs>

            <NewSessionModal
                isOpen={isNewSessionModalOpen}
                onClose={() => setIsNewSessionModalOpen(false)}
                diagramId={diagramId}
            />
        </div>
    );
};
