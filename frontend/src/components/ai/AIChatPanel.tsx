import React, { useState, useEffect, useRef } from 'react';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import { Button } from '@/components/button/button';
import { Textarea } from '@/components/textarea/textarea';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/select/select';
import { useAIAssistantStore } from '@/stores/ai-assistant.store';
import type { AIMessage } from '@/types/ai.types';
import { MessageRole } from '@/types/ai.types';
import {
    Bot,
    User,
    Send,
    Sparkles,
    Loader2,
    Settings,
    Zap,
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useChartDB } from '@/hooks/use-chartdb';
import { useReactFlow } from '@xyflow/react';
import {
    executeAIAction,
    type ActionResult,
} from '@/services/ai-action-executor';
import { AIActionCard } from './AIActionCard';
import { focusOnTable, focusOnRelationship } from '@/lib/diagram-focus-utils';

interface AIChatPanelProps {
    sessionId: string;
    onHighlightTable?: (tableId: string) => void;
}

const quickPrompts = [
    { icon: '📊', text: 'Explain this database schema' },
    { icon: '🔍', text: 'Find relationships between users and orders tables' },
    { icon: '✨', text: 'Suggest improvements for this schema' },
    { icon: '🎯', text: 'Create a new table for products' },
];

const defaultModels: Record<string, string[]> = {
    openai: ['gpt-4', 'gpt-3.5-turbo', 'gpt-4-turbo'],
    gemini: ['gemini-1.5-flash', 'gemini-1.5-pro', 'gemini-3-flash-preview'],
    claude: [
        'claude-3-5-sonnet-20241022',
        'claude-3-opus-20240229',
        'claude-3-sonnet-20240229',
    ],
    mistral: [
        'mistral-small-latest',
        'mistral-large-latest',
        'mistral-medium-latest',
    ],
    deepseek: ['deepseek-chat', 'deepseek-coder'],
};

const MessageBubble: React.FC<{
    message: AIMessage;
    onHighlightTable?: (
        elementId: string,
        elementType: 'table' | 'column' | 'relationship'
    ) => void;
    actionResult?: ActionResult;
}> = ({ message, onHighlightTable, actionResult }) => {
    const isUser = message.role === MessageRole.USER;
    const isSystem = message.role === MessageRole.SYSTEM;

    return (
        <div
            className={`mb-4 flex gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
        >
            <div
                className={`flex size-8 shrink-0 items-center justify-center rounded-full ${
                    isUser
                        ? 'bg-blue-600'
                        : isSystem
                          ? 'bg-gray-500'
                          : 'bg-gradient-to-br from-purple-500 to-pink-500'
                }`}
            >
                {isUser ? (
                    <User className="size-4 text-white" />
                ) : (
                    <Bot className="size-4 text-white" />
                )}
            </div>

            <div
                className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} max-w-[75%]`}
            >
                <span className="mb-1 px-2 text-xs text-gray-500 dark:text-gray-400">
                    {isUser ? 'You' : isSystem ? 'System' : 'AI Assistant'}
                </span>

                <div
                    className={`px-4 py-3 ${
                        isUser
                            ? 'rounded-2xl rounded-tr-md bg-blue-600 text-white'
                            : isSystem
                              ? 'rounded-2xl rounded-tl-md bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'
                              : 'rounded-2xl rounded-tl-md bg-gray-100 text-gray-900 shadow-sm dark:bg-gray-800 dark:text-gray-100'
                    }`}
                >
                    {isUser ? (
                        <p className="whitespace-pre-wrap text-sm leading-relaxed">
                            {message.content}
                        </p>
                    ) : (
                        <div className="prose prose-sm dark:prose-invert prose-p:leading-relaxed prose-pre:bg-gray-900 prose-pre:text-gray-100 max-w-none">
                            <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                {message.content}
                            </ReactMarkdown>
                        </div>
                    )}
                </div>

                {actionResult && (
                    <div className="mt-2 w-full max-w-md">
                        <AIActionCard
                            result={actionResult}
                            onFocus={() =>
                                onHighlightTable?.(
                                    actionResult.targetId || '',
                                    'table'
                                )
                            }
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export const AIChatPanel: React.FC<AIChatPanelProps> = ({
    sessionId,
    onHighlightTable,
}) => {
    const [input, setInput] = useState('');
    const [showModelSelector, setShowModelSelector] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [actionResults, setActionResults] = useState<
        Map<string, ActionResult>
    >(new Map());

    const chartDB = useChartDB();
    const reactFlowInstance = useReactFlow();

    const {
        messages,
        isLoadingMessages,
        isSendingMessage,
        messageError,
        loadMessages,
        sendMessage,
        userConfig,
        loadConfig,
        updateConfig,
    } = useAIAssistantStore();

    // Local state for model selection
    const [selectedProvider, setSelectedProvider] = useState(
        userConfig?.defaultProvider || 'mistral'
    );
    const [selectedModel, setSelectedModel] = useState(
        userConfig?.defaultModel || 'mistral-small-latest'
    );

    // Available providers based on configured API keys
    const availableProviders = [
        { code: 'openai', name: 'OpenAI' },
        { code: 'gemini', name: 'Google Gemini' },
        { code: 'claude', name: 'Anthropic Claude' },
        { code: 'mistral', name: 'Mistral AI' },
        { code: 'deepseek', name: 'DeepSeek' },
    ];

    useEffect(() => {
        if (sessionId) {
            loadMessages(sessionId);
        }
    }, [sessionId, loadMessages]);

    useEffect(() => {
        loadConfig();
    }, [loadConfig]);

    useEffect(() => {
        if (userConfig) {
            setSelectedProvider(userConfig.defaultProvider);
            setSelectedModel(userConfig.defaultModel);
        }
    }, [userConfig]);

    // Update model when provider changes
    useEffect(() => {
        const models = defaultModels[selectedProvider] || [];
        if (models.length > 0 && !models.includes(selectedModel)) {
            setSelectedModel(models[0]);
        }
    }, [selectedProvider, selectedModel]);

    // Execute AI actions when messages arrive
    useEffect(() => {
        const processMessages = async () => {
            for (const message of messages) {
                if (
                    message.metadata?.functionName &&
                    message.metadata?.arguments &&
                    message.metadata?.result?.executeOnFrontend &&
                    !actionResults.has(message.id)
                ) {
                    try {
                        const functionCall = {
                            name: message.metadata.functionName,
                            arguments: message.metadata.arguments,
                        };

                        const result = await executeAIAction(functionCall, {
                            ...chartDB,
                            tables: chartDB.currentDiagram?.tables || [],
                        });

                        setActionResults((prev) =>
                            new Map(prev).set(message.id, result)
                        );
                    } catch (error) {
                        console.error('Failed to execute AI action:', error);
                    }
                }
            }
        };

        processMessages();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [messages]);

    const handleFocusElement = (
        elementId: string,
        elementType: 'table' | 'column' | 'relationship'
    ) => {
        const nodes = reactFlowInstance?.getNodes() || [];
        const edges = reactFlowInstance?.getEdges() || [];

        if (elementType === 'table') {
            focusOnTable(elementId, nodes, reactFlowInstance, {});
        } else if (elementType === 'relationship') {
            focusOnRelationship(elementId, edges, nodes, reactFlowInstance, {});
        }

        if (onHighlightTable) {
            onHighlightTable(elementId);
        }
    };

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isSendingMessage]);

    const handleSendMessage = async () => {
        if (!input.trim() || isSendingMessage) {
            return;
        }

        const messageContent = input.trim();
        setInput('');

        // Update config with selected provider/model if changed
        if (
            selectedProvider !== userConfig?.defaultProvider ||
            selectedModel !== userConfig?.defaultModel
        ) {
            await updateConfig({
                defaultProvider: selectedProvider,
                defaultModel: selectedModel,
            });
        }

        try {
            await sendMessage(sessionId, {
                content: messageContent,
                includeContext: true,
            });
        } catch {
            // Error handled by store
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const providerName =
        selectedProvider === 'openai'
            ? 'OpenAI'
            : selectedProvider === 'gemini'
              ? 'Gemini'
              : selectedProvider === 'claude'
                ? 'Claude'
                : selectedProvider === 'mistral'
                  ? 'Mistral'
                  : selectedProvider === 'deepseek'
                    ? 'DeepSeek'
                    : 'AI';

    return (
        <Card className="flex h-full flex-col">
            <CardHeader className="border-b pb-3">
                <div className="mb-2 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <div className="flex size-10 items-center justify-center rounded-full bg-gradient-to-br from-purple-500 to-pink-500">
                            <Bot className="size-5 text-white" />
                        </div>
                        <div>
                            <CardTitle>AI Assistant</CardTitle>
                            <CardDescription className="text-xs">
                                Powered by {providerName}
                            </CardDescription>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setShowModelSelector(!showModelSelector)}
                        className="h-8"
                    >
                        <Settings className="mr-1 size-3" />
                        <span className="text-xs">
                            {selectedModel || 'Select Model'}
                        </span>
                    </Button>
                </div>

                {showModelSelector && (
                    <div className="mt-3 space-y-3 rounded-lg border bg-gray-50 p-3 dark:bg-gray-900">
                        <div className="grid grid-cols-2 gap-2">
                            <div>
                                <label className="mb-1 block text-xs font-medium">
                                    Provider
                                </label>
                                <Select
                                    value={selectedProvider}
                                    onValueChange={setSelectedProvider}
                                >
                                    <SelectTrigger className="h-8 text-xs">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {availableProviders.map((p) => (
                                            <SelectItem
                                                key={p.code}
                                                value={p.code}
                                                className="text-xs"
                                            >
                                                {p.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div>
                                <label className="mb-1 block text-xs font-medium">
                                    Model
                                </label>
                                <Select
                                    value={selectedModel}
                                    onValueChange={setSelectedModel}
                                >
                                    <SelectTrigger className="h-8 text-xs">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {(
                                            defaultModels[selectedProvider] ||
                                            []
                                        ).map((model) => (
                                            <SelectItem
                                                key={model}
                                                value={model}
                                                className="text-xs"
                                            >
                                                {model}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <p className="text-xs text-gray-500">
                            Switch models for different capabilities. Fast
                            models for quick responses, larger models for
                            complex tasks.
                        </p>
                    </div>
                )}
            </CardHeader>

            <CardContent className="flex flex-1 flex-col overflow-hidden p-0">
                {/* Messages */}
                <div
                    ref={scrollRef}
                    className="flex-1 space-y-4 overflow-y-auto p-4"
                >
                    {isLoadingMessages && (
                        <div className="py-8 text-center text-gray-500">
                            <Loader2 className="mx-auto mb-2 size-8 animate-spin text-purple-500" />
                            <p className="text-sm">Loading conversation...</p>
                        </div>
                    )}

                    {!isLoadingMessages && messages.length === 0 && (
                        <div className="py-8 text-center">
                            <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-gradient-to-br from-purple-500 to-pink-500">
                                <Sparkles className="size-8 text-white" />
                            </div>
                            <h3 className="mb-2 text-lg font-semibold">
                                Start a Conversation
                            </h3>
                            <p className="mb-6 text-sm text-gray-500">
                                Ask me anything about your database schema
                            </p>

                            {/* Quick Prompts */}
                            <div className="mx-auto grid max-w-md grid-cols-1 gap-2">
                                {quickPrompts.map((prompt, idx) => (
                                    <button
                                        key={idx}
                                        onClick={() => setInput(prompt.text)}
                                        className="group flex items-center gap-3 rounded-lg border bg-white px-4 py-3 text-left transition-colors hover:border-purple-500 hover:bg-purple-50 dark:bg-gray-800 dark:hover:bg-purple-900/20"
                                    >
                                        <span className="text-2xl">
                                            {prompt.icon}
                                        </span>
                                        <span className="text-sm text-gray-700 group-hover:text-purple-700 dark:text-gray-300 dark:group-hover:text-purple-400">
                                            {prompt.text}
                                        </span>
                                        <Zap className="ml-auto size-4 text-gray-400 opacity-0 transition-opacity group-hover:text-purple-500 group-hover:opacity-100" />
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {messages.map((message) => (
                        <MessageBubble
                            key={message.id}
                            message={message}
                            onHighlightTable={handleFocusElement}
                            actionResult={actionResults.get(message.id)}
                        />
                    ))}

                    {isSendingMessage && (
                        <div className="flex gap-3">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-purple-500">
                                <Bot className="size-4 text-white" />
                            </div>
                            <div className="flex-1">
                                <div className="inline-block rounded-lg bg-gray-100 px-4 py-3 dark:bg-gray-800">
                                    <div className="flex gap-2">
                                        <div className="size-2 animate-bounce rounded-full bg-gray-400"></div>
                                        <div className="size-2 animate-bounce rounded-full bg-gray-400 [animation-delay:0.2s]"></div>
                                        <div className="size-2 animate-bounce rounded-full bg-gray-400 [animation-delay:0.4s]"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>

                {/* Enhanced Input */}
                <div className="border-t bg-gray-50 p-4 dark:bg-gray-900/50">
                    <div className="flex items-end gap-2">
                        <div className="flex-1">
                            <Textarea
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyDown={handleKeyDown}
                                placeholder="Ask a question or describe what you want to do..."
                                className="max-h-[200px] min-h-[80px] resize-none bg-white dark:bg-gray-800"
                                disabled={isSendingMessage}
                            />
                            <div className="mt-2 flex items-center justify-between">
                                <div className="flex items-center gap-2 text-xs text-gray-500">
                                    <span>
                                        <kbd className="rounded bg-gray-200 px-1.5 py-0.5 text-xs dark:bg-gray-700">
                                            Enter
                                        </kbd>{' '}
                                        to send
                                    </span>
                                    <span>•</span>
                                    <span>
                                        <kbd className="rounded bg-gray-200 px-1.5 py-0.5 text-xs dark:bg-gray-700">
                                            Shift
                                        </kbd>{' '}
                                        +{' '}
                                        <kbd className="rounded bg-gray-200 px-1.5 py-0.5 text-xs dark:bg-gray-700">
                                            Enter
                                        </kbd>{' '}
                                        for new line
                                    </span>
                                </div>
                                <span className="text-xs text-gray-400">
                                    {input.length} chars
                                </span>
                            </div>
                        </div>
                        <Button
                            onClick={handleSendMessage}
                            disabled={!input.trim() || isSendingMessage}
                            className="h-[80px] shrink-0 bg-gradient-to-br from-purple-500 to-pink-500 px-6 hover:from-purple-600 hover:to-pink-600"
                            size="lg"
                        >
                            {isSendingMessage ? (
                                <Loader2 className="size-5 animate-spin" />
                            ) : (
                                <Send className="size-5" />
                            )}
                        </Button>
                    </div>

                    {messageError && (
                        <div className="mt-2 text-xs text-red-500">
                            {messageError}
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
};
