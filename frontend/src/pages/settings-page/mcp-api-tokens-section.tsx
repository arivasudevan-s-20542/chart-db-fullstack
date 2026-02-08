import React, { useEffect, useState, useCallback } from 'react';
import { Button } from '@/components/button/button';
import { Input } from '@/components/input/input';
import { Label } from '@/components/label/label';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import { Alert, AlertDescription } from '@/components/alert/alert';
import { mcpTokenApi, type McpApiToken } from '@/services/api';
import {
    Loader2,
    Plus,
    Trash2,
    Copy,
    Check,
    AlertCircle,
    Key,
    Clock,
    Shield,
} from 'lucide-react';

export const McpApiTokensSection: React.FC = () => {
    const [tokens, setTokens] = useState<McpApiToken[]>([]);
    const [loading, setLoading] = useState(true);
    const [creating, setCreating] = useState(false);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [newTokenName, setNewTokenName] = useState('');
    const [newTokenExpiry, setNewTokenExpiry] = useState('');
    const [newlyCreatedToken, setNewlyCreatedToken] = useState<string | null>(
        null
    );
    const [copiedToken, setCopiedToken] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const loadTokens = useCallback(async () => {
        try {
            setLoading(true);
            const data = await mcpTokenApi.listTokens();
            setTokens(data);
        } catch (err) {
            console.error('Failed to load MCP tokens:', err);
            setError('Failed to load API tokens');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadTokens();
    }, [loadTokens]);

    const handleCreate = async () => {
        setError('');
        setSuccess('');

        if (!newTokenName.trim()) {
            setError('Please enter a token name');
            return;
        }

        try {
            setCreating(true);
            const result = await mcpTokenApi.createToken({
                name: newTokenName.trim(),
                expiresAt: newTokenExpiry
                    ? new Date(newTokenExpiry).toISOString()
                    : undefined,
            });

            setNewlyCreatedToken(result.token || null);
            setNewTokenName('');
            setNewTokenExpiry('');
            setShowCreateForm(false);
            setSuccess("Token created! Copy it now — it won't be shown again.");
            await loadTokens();
        } catch (err: unknown) {
            console.error('Failed to create token:', err);
            const e = err as { response?: { data?: { message?: string } } };
            setError(e.response?.data?.message || 'Failed to create API token');
        } finally {
            setCreating(false);
        }
    };

    const handleRevoke = async (tokenId: string, tokenName: string) => {
        if (
            !confirm(
                `Are you sure you want to revoke the token "${tokenName}"?`
            )
        ) {
            return;
        }

        try {
            await mcpTokenApi.revokeToken(tokenId);
            setSuccess(`Token "${tokenName}" revoked successfully`);
            await loadTokens();
        } catch (err: unknown) {
            console.error('Failed to revoke token:', err);
            const e = err as { response?: { data?: { message?: string } } };
            setError(e.response?.data?.message || 'Failed to revoke token');
        }
    };

    const handleCopyToken = async (token: string) => {
        try {
            await navigator.clipboard.writeText(token);
            setCopiedToken(true);
            setTimeout(() => setCopiedToken(false), 2000);
        } catch {
            // Fallback: select text
            const textArea = document.createElement('textarea');
            textArea.value = token;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            setCopiedToken(true);
            setTimeout(() => setCopiedToken(false), 2000);
        }
    };

    const formatDate = (dateStr: string | null) => {
        if (!dateStr) return 'Never';
        return new Date(dateStr).toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    return (
        <Card>
            <CardHeader>
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="flex items-center gap-2">
                            <Key className="size-5" />
                            MCP API Tokens
                        </CardTitle>
                        <CardDescription className="mt-1">
                            Generate API tokens for MCP clients like Claude
                            Desktop, Cursor, or custom integrations to access
                            your ChartDB data programmatically.
                        </CardDescription>
                    </div>
                    {!showCreateForm && (
                        <Button
                            size="sm"
                            onClick={() => {
                                setShowCreateForm(true);
                                setNewlyCreatedToken(null);
                                setError('');
                                setSuccess('');
                            }}
                        >
                            <Plus className="mr-2 size-4" />
                            New Token
                        </Button>
                    )}
                </div>
            </CardHeader>
            <CardContent className="space-y-4">
                {error && (
                    <Alert variant="destructive">
                        <AlertCircle className="size-4" />
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}

                {success && (
                    <Alert className="border-green-500 bg-green-50 text-green-900">
                        <Check className="size-4" />
                        <AlertDescription>{success}</AlertDescription>
                    </Alert>
                )}

                {/* Newly created token display */}
                {newlyCreatedToken && (
                    <Alert className="border-amber-500 bg-amber-50 text-amber-900">
                        <Shield className="size-4" />
                        <AlertDescription>
                            <div className="space-y-2">
                                <p className="font-semibold">
                                    Your new API token (copy it now — it
                                    won&apos;t be shown again):
                                </p>
                                <div className="flex items-center gap-2">
                                    <code className="flex-1 break-all rounded bg-amber-100 px-3 py-2 font-mono text-xs">
                                        {newlyCreatedToken}
                                    </code>
                                    <Button
                                        size="sm"
                                        variant="outline"
                                        onClick={() =>
                                            handleCopyToken(newlyCreatedToken)
                                        }
                                    >
                                        {copiedToken ? (
                                            <Check className="size-4" />
                                        ) : (
                                            <Copy className="size-4" />
                                        )}
                                    </Button>
                                </div>
                                <p className="mt-2 text-xs">
                                    <strong>Usage with MCP clients:</strong> Set
                                    <code className="rounded bg-amber-100 px-1">
                                        Authorization: Bearer{' '}
                                        {newlyCreatedToken.substring(0, 12)}...
                                    </code>
                                    in your MCP client configuration.
                                </p>
                            </div>
                        </AlertDescription>
                    </Alert>
                )}

                {/* Create form */}
                {showCreateForm && (
                    <div className="space-y-3 rounded-lg border p-4">
                        <h4 className="font-medium">Create New API Token</h4>
                        <div className="space-y-2">
                            <Label htmlFor="token-name">Token Name</Label>
                            <Input
                                id="token-name"
                                placeholder="e.g., Claude Desktop, My Script"
                                value={newTokenName}
                                onChange={(e) =>
                                    setNewTokenName(e.target.value)
                                }
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') handleCreate();
                                }}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="token-expiry">
                                Expiration (optional)
                            </Label>
                            <Input
                                id="token-expiry"
                                type="datetime-local"
                                value={newTokenExpiry}
                                onChange={(e) =>
                                    setNewTokenExpiry(e.target.value)
                                }
                            />
                            <p className="text-xs text-muted-foreground">
                                Leave blank for a token that never expires.
                            </p>
                        </div>
                        <div className="flex gap-2 pt-2">
                            <Button
                                onClick={handleCreate}
                                disabled={creating || !newTokenName.trim()}
                                size="sm"
                            >
                                {creating ? (
                                    <>
                                        <Loader2 className="mr-2 size-4 animate-spin" />
                                        Creating...
                                    </>
                                ) : (
                                    <>
                                        <Plus className="mr-2 size-4" />
                                        Create Token
                                    </>
                                )}
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => {
                                    setShowCreateForm(false);
                                    setNewTokenName('');
                                    setNewTokenExpiry('');
                                }}
                            >
                                Cancel
                            </Button>
                        </div>
                    </div>
                )}

                {/* Token list */}
                {loading ? (
                    <div className="flex items-center justify-center py-8">
                        <Loader2 className="size-6 animate-spin text-muted-foreground" />
                    </div>
                ) : tokens.length === 0 ? (
                    <div className="py-8 text-center text-muted-foreground">
                        <Key className="mx-auto mb-3 size-10 opacity-50" />
                        <p>No API tokens yet.</p>
                        <p className="text-sm">
                            Create a token to allow MCP clients to access your
                            ChartDB data.
                        </p>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {tokens.map((token) => (
                            <div
                                key={token.id}
                                className={`flex items-center justify-between rounded-lg border p-3 ${
                                    !token.active
                                        ? 'opacity-50 bg-muted/30'
                                        : ''
                                }`}
                            >
                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-2">
                                        <span className="font-medium truncate">
                                            {token.name}
                                        </span>
                                        {token.active ? (
                                            <span className="inline-flex items-center rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
                                                Active
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                                                Revoked
                                            </span>
                                        )}
                                    </div>
                                    <div className="mt-1 flex items-center gap-4 text-xs text-muted-foreground">
                                        <span className="font-mono">
                                            {token.tokenPrefix}•••
                                        </span>
                                        <span className="flex items-center gap-1">
                                            <Clock className="size-3" />
                                            Created{' '}
                                            {formatDate(token.createdAt)}
                                        </span>
                                        {token.lastUsedAt && (
                                            <span>
                                                Last used{' '}
                                                {formatDate(token.lastUsedAt)}
                                            </span>
                                        )}
                                        {token.expiresAt && (
                                            <span>
                                                Expires{' '}
                                                {formatDate(token.expiresAt)}
                                            </span>
                                        )}
                                    </div>
                                </div>
                                {token.active && (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        className="ml-2 text-destructive hover:bg-destructive/10 hover:text-destructive"
                                        onClick={() =>
                                            handleRevoke(token.id, token.name)
                                        }
                                    >
                                        <Trash2 className="size-4" />
                                    </Button>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* MCP Configuration Help */}
                <div className="mt-4 rounded-lg border border-dashed p-4">
                    <h4 className="mb-2 text-sm font-medium">
                        📖 How to use with MCP clients
                    </h4>
                    <div className="space-y-2 text-xs text-muted-foreground">
                        <p>
                            <strong>Claude Desktop</strong>
                            (claude_desktop_config.json):
                        </p>
                        <pre className="overflow-x-auto rounded bg-muted p-2 text-[11px]">
{`{
  "mcpServers": {
    "chartdb": {
      "url": "${window.location.origin}/api/mcp",
      "headers": {
        "Authorization": "Bearer <your-token>"
      }
    }
  }
}`}
                        </pre>
                        <p className="mt-2">
                            <strong>cURL test:</strong>
                        </p>
                        <pre className="overflow-x-auto rounded bg-muted p-2 text-[11px]">
{`curl -H "Authorization: Bearer <your-token>" \\
  ${window.location.origin}/api/mcp/.well-known/mcp.json`}
                        </pre>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
};
