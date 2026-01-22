import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/auth-context';
import { Button } from '@/components/button/button';
import { Input } from '@/components/input/input';
import { Label } from '@/components/label/label';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/select/select';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import {
    aiConfigApi,
    type AIProvider,
    type AIConfig,
    type AIConfigResponse,
} from '@/services/api';
import {
    Loader2,
    Save,
    Trash2,
    Eye,
    EyeOff,
    Check,
    AlertCircle,
    ArrowLeft,
} from 'lucide-react';
import { Alert, AlertDescription } from '@/components/alert/alert';

export const SettingsPage: React.FC = () => {
    const { isAuthenticated, isLoading: authLoading } = useAuth();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [providers, setProviders] = useState<AIProvider[]>([]);
    const [currentConfig, setCurrentConfig] = useState<AIConfigResponse | null>(
        null
    );

    const [selectedProvider, setSelectedProvider] = useState('');
    const [apiKey, setApiKey] = useState('');
    const [selectedModel, setSelectedModel] = useState('');
    const [showApiKey, setShowApiKey] = useState(false);

    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (!authLoading && !isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, authLoading, navigate]);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const [providersData, configData] = await Promise.all([
                aiConfigApi.getProviders(),
                aiConfigApi.getConfig(),
            ]);

            setProviders(providersData);
            setCurrentConfig(configData);

            if (configData.configured && configData.config) {
                setSelectedProvider(configData.config.provider);
                setApiKey(configData.config.apiKey);
                setSelectedModel(configData.config.model);
            }
        } catch (err) {
            console.error('Failed to load AI config:', err);
            setError('Failed to load configuration');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setError('');
        setSuccess('');

        if (!selectedProvider) {
            setError('Please select a provider');
            return;
        }

        const effectiveApiKey =
            selectedProvider === 'mistral' ? mistralApiKey : apiKey;
        if (!effectiveApiKey || effectiveApiKey.includes('*')) {
            setError('Please enter a valid API key');
            return;
        }

        if (!selectedModel) {
            setError('Please select a model');
            return;
        }

        try {
            setSaving(true);
            const config: AIConfig = {
                provider: selectedProvider,
                apiKey: effectiveApiKey,
                model: selectedModel,
            };

            await aiConfigApi.saveConfig(config);
            setSuccess('Configuration saved successfully!');

            // Reload to get masked API key
            await loadData();
        } catch (err: any) {
            console.error('Failed to save config:', err);
            setError(
                err.response?.data?.message || 'Failed to save configuration'
            );
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        if (
            !confirm('Are you sure you want to delete your AI configuration?')
        ) {
            return;
        }

        try {
            setSaving(true);
            await aiConfigApi.deleteConfig();
            setSuccess('Configuration deleted successfully');
            setSelectedProvider('');
            setApiKey('');
            setSelectedModel('');
            setCurrentConfig(null);
        } catch (err: any) {
            console.error('Failed to delete config:', err);
            setError(
                err.response?.data?.message || 'Failed to delete configuration'
            );
        } finally {
            setSaving(false);
        }
    };

    const selectedProviderData = providers.find(
        (p) => p.code === selectedProvider
    );
    const defaultModels: Record<string, string[]> = {
        openai: ['gpt-4', 'gpt-3.5-turbo', 'gpt-4-turbo'],
        gemini: [
            'gemini-3-flash-preview',
            'gemini-1.5-flash',
            'gemini-1.5-pro',
            'gemini-1.5-flash-8b',
        ],
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

    const isMistralProvider = selectedProvider === 'mistral';
    const mistralApiKey =
        '82be75bab472c188118d3857ca2c07fc11365c4d2c23284f9fbf75e2cd79f717';

    if (loading || authLoading) {
        return (
            <div className="flex h-screen items-center justify-center">
                <Loader2 className="size-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    return (
        <div className="container mx-auto max-w-4xl p-6">
            <div className="mb-6">
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate('/')}
                    className="mb-4"
                >
                    <ArrowLeft className="mr-2 size-4" />
                    Back to Editor
                </Button>
                <h1 className="text-3xl font-bold">Settings</h1>
                <p className="text-muted-foreground">
                    Configure your AI assistant preferences
                </p>
            </div>

            {error && (
                <Alert variant="destructive" className="mb-4">
                    <AlertCircle className="size-4" />
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {success && (
                <Alert className="mb-4 border-green-500 bg-green-50 text-green-900">
                    <Check className="size-4" />
                    <AlertDescription>{success}</AlertDescription>
                </Alert>
            )}

            <Card className="mb-6">
                <CardHeader>
                    <CardTitle>AI Provider Configuration</CardTitle>
                    <CardDescription>
                        Choose your preferred AI provider and configure your API
                        key. You'll need an API key from OpenAI, Google Gemini,
                        Anthropic Claude, or use our hosted Mistral AI.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="provider">AI Provider</Label>
                        <Select
                            value={selectedProvider}
                            onValueChange={(value) => {
                                setSelectedProvider(value);
                                // Auto-select model for Mistral or DeepSeek
                                if (value === 'mistral') {
                                    setSelectedModel('mistral-small-latest');
                                } else if (value === 'deepseek') {
                                    setSelectedModel('deepseek-chat');
                                } else {
                                    setSelectedModel('');
                                }
                            }}
                        >
                            <SelectTrigger id="provider">
                                <SelectValue placeholder="Select a provider" />
                            </SelectTrigger>
                            <SelectContent>
                                {providers.map((provider) => (
                                    <SelectItem
                                        key={provider.code}
                                        value={provider.code}
                                    >
                                        {provider.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {selectedProvider && (
                        <>
                            <div className="space-y-2">
                                <Label htmlFor="apiKey">API Key</Label>
                                <div className="flex gap-2">
                                    <div className="relative flex-1">
                                        <Input
                                            id="apiKey"
                                            type={
                                                showApiKey ? 'text' : 'password'
                                            }
                                            value={
                                                isMistralProvider
                                                    ? mistralApiKey
                                                    : apiKey
                                            }
                                            onChange={(e) =>
                                                setApiKey(e.target.value)
                                            }
                                            placeholder={`Enter your ${selectedProviderData?.name} API key`}
                                            className="pr-10"
                                            disabled={isMistralProvider}
                                        />
                                        <button
                                            type="button"
                                            onClick={() =>
                                                setShowApiKey(!showApiKey)
                                            }
                                            className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                        >
                                            {showApiKey ? (
                                                <EyeOff className="size-4" />
                                            ) : (
                                                <Eye className="size-4" />
                                            )}
                                        </button>
                                    </div>
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    {selectedProvider === 'openai' &&
                                        'Get your API key from https://platform.openai.com/api-keys'}
                                    {selectedProvider === 'gemini' &&
                                        'Get your API key from https://makersuite.google.com/app/apikey'}
                                    {selectedProvider === 'claude' &&
                                        'Get your API key from https://console.anthropic.com/settings/keys'}
                                    {selectedProvider === 'mistral' &&
                                        'Mistral AI is hosted on our servers - use the provided API key'}
                                </p>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="model">Model</Label>
                                <Select
                                    value={selectedModel}
                                    onValueChange={setSelectedModel}
                                >
                                    <SelectTrigger id="model">
                                        <SelectValue placeholder="Select a model" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {(
                                            selectedProviderData?.models ||
                                            defaultModels[selectedProvider] ||
                                            []
                                        ).map((model) => (
                                            <SelectItem
                                                key={model}
                                                value={model}
                                            >
                                                {model}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex gap-2 pt-4">
                                <Button
                                    onClick={handleSave}
                                    disabled={
                                        saving ||
                                        !selectedProvider ||
                                        !selectedModel ||
                                        (!isMistralProvider && !apiKey)
                                    }
                                >
                                    {saving ? (
                                        <>
                                            <Loader2 className="mr-2 size-4 animate-spin" />
                                            Saving...
                                        </>
                                    ) : (
                                        <>
                                            <Save className="mr-2 size-4" />
                                            Save Configuration
                                        </>
                                    )}
                                </Button>
                                {currentConfig?.configured && (
                                    <Button
                                        variant="destructive"
                                        onClick={handleDelete}
                                        disabled={saving}
                                    >
                                        <Trash2 className="mr-2 size-4" />
                                        Delete
                                    </Button>
                                )}
                            </div>
                        </>
                    )}
                </CardContent>
            </Card>

            {currentConfig?.configured && (
                <Card>
                    <CardHeader>
                        <CardTitle>Usage Statistics</CardTitle>
                        <CardDescription>
                            Your AI assistant usage information
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                            <div>
                                <p className="text-sm text-muted-foreground">
                                    Total Tokens
                                </p>
                                <p className="text-2xl font-bold">
                                    {(
                                        currentConfig.usageStats?.totalTokens ||
                                        0
                                    ).toLocaleString()}
                                </p>
                            </div>
                            <div>
                                <p className="text-sm text-muted-foreground">
                                    Total Requests
                                </p>
                                <p className="text-2xl font-bold">
                                    {currentConfig.usageStats?.totalRequests ||
                                        0}
                                </p>
                            </div>
                            <div>
                                <p className="text-sm text-muted-foreground">
                                    Last Used
                                </p>
                                <p className="text-2xl font-bold">
                                    {currentConfig.usageStats?.lastUsed
                                        ? new Date(
                                              currentConfig.usageStats.lastUsed
                                          ).toLocaleDateString()
                                        : 'Never'}
                                </p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    );
};
