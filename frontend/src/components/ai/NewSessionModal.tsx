import React, { useState } from 'react';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/dialog/dialog';
import { Button } from '@/components/button/button';
import { Label } from '@/components/label/label';
import { Textarea } from '@/components/textarea/textarea';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/select/select';
import { useAIAssistantStore } from '@/stores/ai-assistant.store';
import type { StartChatSessionRequest } from '@/types/ai.types';
import { Bot, Sparkles } from 'lucide-react';

interface NewSessionModalProps {
    isOpen: boolean;
    onClose: () => void;
    diagramId: string;
}

const PRESET_AGENTS = [
    {
        name: 'Schema Designer',
        description: 'Helps design and optimize database schemas',
        systemPrompt:
            'You are an expert database schema designer. Help users create well-structured, normalized database schemas.',
    },
    {
        name: 'Query Optimizer',
        description: 'Suggests performance improvements and indexes',
        systemPrompt:
            'You are a database performance expert. Analyze schemas and suggest optimization strategies, indexes, and query patterns.',
    },
    {
        name: 'Migration Assistant',
        description: 'Helps plan and execute schema migrations',
        systemPrompt:
            'You are a database migration expert. Help users plan safe schema migrations with minimal downtime.',
    },
    {
        name: 'Documentation Writer',
        description: 'Generates documentation for database schemas',
        systemPrompt:
            'You are a technical writer specializing in database documentation. Create clear, comprehensive documentation.',
    },
];

export const NewSessionModal: React.FC<NewSessionModalProps> = ({
    isOpen,
    onClose,
    diagramId,
}) => {
    const [agentType, setAgentType] = useState<'preset' | 'custom'>('preset');
    const [selectedAgent, setSelectedAgent] = useState(PRESET_AGENTS[0].name);
    const [customSystemPrompt, setCustomSystemPrompt] = useState('');

    const { startSession } = useAIAssistantStore();

    const handleStart = async () => {
        const request: StartChatSessionRequest = {
            diagramId,
        };

        if (agentType === 'preset') {
            const agent = PRESET_AGENTS.find((a) => a.name === selectedAgent);
            if (agent) {
                request.agentName = agent.name;
                request.systemPrompt = agent.systemPrompt;
            }
        } else {
            request.agentName = 'Custom Agent';
            request.systemPrompt = customSystemPrompt;
        }

        try {
            await startSession(request);
            onClose();
            // Reset form
            setAgentType('preset');
            setSelectedAgent(PRESET_AGENTS[0].name);
            setCustomSystemPrompt('');
        } catch {
            // Error is handled by the store
        }
    };

    const selectedPresetAgent = PRESET_AGENTS.find(
        (a) => a.name === selectedAgent
    );

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle>Start AI Conversation</DialogTitle>
                </DialogHeader>

                <div className="space-y-6 py-4">
                    {/* Agent Type Selection */}
                    <div className="grid grid-cols-2 gap-3">
                        <button
                            onClick={() => setAgentType('preset')}
                            className={`rounded-lg border-2 p-4 transition-colors ${
                                agentType === 'preset'
                                    ? 'border-purple-500 bg-purple-50 dark:bg-purple-950'
                                    : 'border-gray-200 hover:border-gray-300'
                            }`}
                        >
                            <Bot className="mx-auto mb-2 size-8 text-purple-500" />
                            <p className="text-sm font-medium">Preset Agent</p>
                            <p className="mt-1 text-xs text-gray-500">
                                Choose from expert agents
                            </p>
                        </button>

                        <button
                            onClick={() => setAgentType('custom')}
                            className={`rounded-lg border-2 p-4 transition-colors ${
                                agentType === 'custom'
                                    ? 'border-purple-500 bg-purple-50 dark:bg-purple-950'
                                    : 'border-gray-200 hover:border-gray-300'
                            }`}
                        >
                            <Sparkles className="mx-auto mb-2 size-8 text-purple-500" />
                            <p className="text-sm font-medium">Custom Agent</p>
                            <p className="mt-1 text-xs text-gray-500">
                                Create your own agent
                            </p>
                        </button>
                    </div>

                    {/* Preset Agent Selection */}
                    {agentType === 'preset' && (
                        <div className="space-y-3">
                            <Label>Select Agent</Label>
                            <Select
                                value={selectedAgent}
                                onValueChange={setSelectedAgent}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {PRESET_AGENTS.map((agent) => (
                                        <SelectItem
                                            key={agent.name}
                                            value={agent.name}
                                        >
                                            {agent.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>

                            {selectedPresetAgent && (
                                <div className="rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
                                    <p className="text-sm text-gray-600 dark:text-gray-400">
                                        {selectedPresetAgent.description}
                                    </p>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Custom Agent */}
                    {agentType === 'custom' && (
                        <div className="space-y-3">
                            <Label htmlFor="systemPrompt">System Prompt</Label>
                            <Textarea
                                id="systemPrompt"
                                value={customSystemPrompt}
                                onChange={(e) =>
                                    setCustomSystemPrompt(e.target.value)
                                }
                                placeholder="You are an expert assistant that helps with..."
                                className="min-h-[120px]"
                            />
                            <p className="text-xs text-gray-500">
                                Define the behavior and expertise of your custom
                                AI agent
                            </p>
                        </div>
                    )}

                    {/* Info Box */}
                    <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 dark:border-blue-800 dark:bg-blue-950">
                        <p className="text-xs text-blue-700 dark:text-blue-300">
                            💡 The AI assistant will automatically include your
                            current diagram context in every conversation
                        </p>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button
                        onClick={handleStart}
                        disabled={
                            agentType === 'custom' && !customSystemPrompt.trim()
                        }
                    >
                        Start Chat
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
