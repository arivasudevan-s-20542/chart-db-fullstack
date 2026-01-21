import React, { useState } from 'react';
import { ConnectionModal } from './ConnectionModal';
import { ConnectionList } from './ConnectionList';
import { QueryEditor } from './QueryEditor';
import { DatabaseConnection } from '@/types/database.types';
import { useDatabaseConnectionStore } from '@/stores/database-connection.store';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/tabs/tabs';

interface DatabasePanelProps {
    diagramId: string;
}

export const DatabasePanel: React.FC<DatabasePanelProps> = ({ diagramId }) => {
    const [isConnectionModalOpen, setIsConnectionModalOpen] = useState(false);
    const { connections, activeConnectionId, setActiveConnection } = useDatabaseConnectionStore();

    const activeConnection = connections.find((c) => c.id === activeConnectionId);

    const handleSelectConnection = (connection: DatabaseConnection) => {
        setActiveConnection(connection.id);
    };

    return (
        <div className="h-full flex flex-col">
            <Tabs defaultValue="connections" className="flex-1 flex flex-col">
                <TabsList className="w-full justify-start border-b rounded-none bg-transparent p-0">
                    <TabsTrigger value="connections" className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-500">
                        Connections
                    </TabsTrigger>
                    <TabsTrigger 
                        value="query" 
                        className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-500"
                        disabled={!activeConnection}
                    >
                        Query Editor
                    </TabsTrigger>
                </TabsList>

                <div className="flex-1 overflow-auto p-4">
                    <TabsContent value="connections" className="mt-0">
                        <ConnectionList
                            diagramId={diagramId}
                            onAddConnection={() => setIsConnectionModalOpen(true)}
                            onSelectConnection={handleSelectConnection}
                        />
                    </TabsContent>

                    <TabsContent value="query" className="mt-0 h-full">
                        {activeConnection ? (
                            <QueryEditor connection={activeConnection} />
                        ) : (
                            <div className="text-center py-12 text-gray-500">
                                Select a connection to start querying
                            </div>
                        )}
                    </TabsContent>
                </div>
            </Tabs>

            <ConnectionModal
                isOpen={isConnectionModalOpen}
                onClose={() => setIsConnectionModalOpen(false)}
                diagramId={diagramId}
            />
        </div>
    );
};
