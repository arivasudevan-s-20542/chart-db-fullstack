import React, { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/card/card';
import { Button } from '@/components/button/button';
import { Badge } from '@/components/badge/badge';
import { Textarea } from '@/components/textarea/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/select/select';
import { useAIAssistantStore } from '@/stores/ai-assistant.store';
import { AIMessage, MessageRole } from '@/types/ai.types';
import { Bot, User, Send, Sparkles, Loader2, Settings, Zap } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useChartDB } from '@/hooks/use-chartdb';
import { useReactFlow } from '@xyflow/react';
import { executeAIAction, type ActionResult } from '@/services/ai-action-executor';
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
    claude: ['claude-3-5-sonnet-20241022', 'claude-3-opus-20240229', 'claude-3-sonnet-20240229'],
    mistral: ['mistral-small-latest', 'mistral-large-latest', 'mistral-medium-latest'],
    deepseek: ['deepseek-chat', 'deepseek-coder'],
};

const MessageBubble: React.FC<{
    message: AIMessage;
    onHighlightTable?: (elementId: string, elementType: 'table' | 'column' | 'relationship') => void;
    actionResult?: ActionResult;
}> = ({ message, onHighlightTable, actionResult }) => {
    const isUser = message.role === MessageRole.USER;
    const isSystem = message.role === MessageRole.SYSTEM;

    return (
        <div className={`flex gap-3 mb-4 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
            <div
                className={`shrink-0 h-8 w-8 rounded-full flex items-center justify-center ${
                    isUser 
                        ? 'bg-blue-600' 
                        : isSystem 
                          ? 'bg-gray-500' 
                          : 'bg-gradient-to-br from-purple-500 to-pink-500'
                }`}
            >
                {isUser ? <User className="h-4 w-4 text-white" /> : <Bot className="h-4 w-4 text-white" />}
            </div>

            <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} max-w-[75%]`}>
                <span className="text-xs text-gray-500 dark:text-gray-400 mb-1 px-2">
                    {isUser ? 'You' : isSystem ? 'System' : 'AI Assistant'}
                </span>
                
                <div
                    className={`px-4 py-3 ${
                        isUser
                            ? 'bg-blue-600 text-white rounded-2xl rounded-tr-md'
                            : isSystem
                              ? 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 rounded-2xl rounded-tl-md'
                              : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-2xl rounded-tl-md shadow-sm'
                    }`}
                >
                    {isUser ? (
                        <p className="whitespace-pre-wrap text-sm leading-relaxed">{message.content}</p>
                    ) : (
                        <div className="prose prose-sm max-w-none dark:prose-invert prose-p:leading-relaxed prose-pre:bg-gray-900 prose-pre:text-gray-100">
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
                            onFocus={() => onHighlightTable?.(actionResult.targetId || '', 'table')}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export const AIChatPanel: React.FC<AIChatPanelProps> = ({ sessionId, onHighlightTable }) => {
    const [input, setInput] = useState('');
    const [showModelSelector, setShowModelSelector] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [actionResults, setActionResults] = useState<Map<string, ActionResult>>(new Map());
    
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
        updateConfig 
    } = useAIAssistantStore();

    // Local state for model selection
    const [selectedProvider, setSelectedProvider] = useState(userConfig?.defaultProvider || 'mistral');
    const [selectedModel, setSelectedModel] = useState(userConfig?.defaultModel || 'mistral-small-latest');

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
                if (message.metadata?.functionName && 
                    message.metadata?.arguments && 
                    message.metadata?.result?.executeOnFrontend &&
                    !actionResults.has(message.id)) {
                    
                    try {
                        const functionCall = {
                            name: message.metadata.functionName,
                            arguments: message.metadata.arguments,
                        };
                        
                        const result = await executeAIAction(functionCall, {
                            ...chartDB,
                            tables: chartDB.currentDiagram?.tables || [],
                        });
                        
                        setActionResults(prev => new Map(prev).set(message.id, result));
                    } catch (error) {
                        console.error('Failed to execute AI action:', error);
                    }
                }
            }
        };
        
        processMessages();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [messages]);
    
    const handleFocusElement = (elementId: string, elementType: 'table' | 'column' | 'relationship') => {
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
        if (selectedProvider !== userConfig?.defaultProvider || selectedModel !== userConfig?.defaultModel) {
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
        } catch (error) {
            // Error handled by store
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const providerName = selectedProvider === 'openai' ? 'OpenAI' :
                         selectedProvider === 'gemini' ? 'Gemini' :
                         selectedProvider === 'claude' ? 'Claude' :
                         selectedProvider === 'mistral' ? 'Mistral' :
                         selectedProvider === 'deepseek' ? 'DeepSeek' : 'AI';

    return (
        <Card className="h-full flex flex-col">
            <CardHeader className="border-b pb-3">
                <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                        <div className="h-10 w-10 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center">
                            <Bot className="h-5 w-5 text-white" />
                        </div>
                        <div>
                            <CardTitle>AI Assistant</CardTitle>
                            <CardDescription className="text-xs">Powered by {providerName}</CardDescription>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setShowModelSelector(!showModelSelector)}
                        className="h-8"
                    >
                        <Settings className="mr-1 h-3 w-3" />
                        <span className="text-xs">{selectedModel || 'Select Model'}</span>
                    </Button>
                </div>
                
                {showModelSelector && (
                    <div className="mt-3 p-3 bg-gray-50 dark:bg-gray-900 rounded-lg border space-y-3">
                        <div className="grid grid-cols-2 gap-2">
                            <div>
                                <label className="text-xs font-medium mb-1 block">Provider</label>
                                <Select value={selectedProvider} onValueChange={setSelectedProvider}>
                                    <SelectTrigger className="h-8 text-xs">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {availableProviders.map((p) => (
                                            <SelectItem key={p.code} value={p.code} className="text-xs">
                                                {p.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div>
                                <label className="text-xs font-medium mb-1 block">Model</label>
                                <Select value={selectedModel} onValueChange={setSelectedModel}>
                                    <SelectTrigger className="h-8 text-xs">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {(defaultModels[selectedProvider] || []).map((model) => (
                                            <SelectItem key={model} value={model} className="text-xs">
                                                {model}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <p className="text-xs text-gray-500">
                            Switch models for different capabilities. Fast models for quick responses, larger models for complex tasks.
                        </p>
                    </div>
                )}
            </CardHeader>

            <CardContent className="flex-1 flex flex-col p-0 overflow-hidden">
                {/* Messages */}
                <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-4">
                    {isLoadingMessages && (
                        <div className="text-center py-8 text-gray-500">
                            <Loader2 className="mx-auto h-8 w-8 animate-spin text-purple-500 mb-2" />
                            <p className="text-sm">Loading conversation...</p>
                        </div>
                    )}

                    {!isLoadingMessages && messages.length === 0 && (
                        <div className="text-center py-8">
                            <div className="h-16 w-16 mx-auto rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center mb-4">
                                <Sparkles className="h-8 w-8 text-white" />
                            </div>
                            <h3 className="text-lg font-semibold mb-2">Start a Conversation</h3>
                            <p className="text-sm text-gray-500 mb-6">
                                Ask me anything about your database schema
                            </p>
                            
                            {/* Quick Prompts */}
                            <div className="grid grid-cols-1 gap-2 max-w-md mx-auto">
                                {quickPrompts.map((prompt, idx) => (
                                    <button
                                        key={idx}
                                        onClick={() => setInput(prompt.text)}
                                        className="flex items-center gap-3 px-4 py-3 bg-white dark:bg-gray-800 border rounded-lg hover:border-purple-500 hover:bg-purple-50 dark:hover:bg-purple-900/20 transition-colors text-left group"
                                    >
                                        <span className="text-2xl">{prompt.icon}</span>
                                        <span className="text-sm text-gray-700 dark:text-gray-300 group-hover:text-purple-700 dark:group-hover:text-purple-400">
                                            {prompt.text}
                                        </span>
                                        <Zap className="ml-auto h-4 w-4 text-gray-400 group-hover:text-purple-500 opacity-0 group-hover:opacity-100 transition-opacity" />
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
                            <div className="shrink-0 h-8 w-8 rounded-full bg-purple-500 flex items-center justify-center">
                                <Bot className="h-4 w-4 text-white" />
                            </div>
                            <div className="flex-1">
                                <div className="inline-block px-4 py-3 rounded-lg bg-gray-100 dark:bg-gray-800">
                                    <div className="flex gap-2">
                                        <div className="h-2 w-2 bg-gray-400 rounded-full animate-bounce"></div>
                                        <div className="h-2 w-2 bg-gray-400 rounded-full animate-bounce [animation-delay:0.2s]"></div>
                                        <div className="h-2 w-2 bg-gray-400 rounded-full animate-bounce [animation-delay:0.4s]"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>

                {/* Enhanced Input */}
                <div className="border-t p-4 bg-gray-50 dark:bg-gray-900/50">
                    <div className="flex gap-2 items-end">
                        <div className="flex-1">
                            <Textarea
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyDown={handleKeyDown}
                                placeholder="Ask a question or describe what you want to do..."
                                className="min-h-[80px] max-h-[200px] resize-none bg-white dark:bg-gray-800"
                                disabled={isSendingMessage}
                            />
                            <div className="flex items-center justify-between mt-2">
                                <div className="flex items-center gap-2 text-xs text-gray-500">
                                    <span>
                                        <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded text-xs">Enter</kbd> to send
                                    </span>
                                    <span>•</span>
                                    <span>
                                        <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded text-xs">Shift</kbd> +{' '}
                                        <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded text-xs">Enter</kbd> for new line
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
                            className="shrink-0 h-[80px] px-6 bg-gradient-to-br from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600"
                            size="lg"
                        >
                            {isSendingMessage ? (
                                <Loader2 className="h-5 w-5 animate-spin" />
                            ) : (
                                <Send className="h-5 w-5" />
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
