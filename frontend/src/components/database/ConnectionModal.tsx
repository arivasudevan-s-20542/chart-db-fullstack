import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/dialog/dialog';
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
import { Alert, AlertDescription } from '@/components/alert/alert';
import { useDatabaseConnectionStore } from '@/stores/database-connection.store';
import type { CreateConnectionRequest } from '@/types/database.types';
import { DatabaseType } from '@/types/database.types';
import { Loader2, Check, X } from 'lucide-react';

interface ConnectionModalProps {
    isOpen: boolean;
    onClose: () => void;
    diagramId: string;
}

const DEFAULT_PORTS: Record<DatabaseType, number> = {
    [DatabaseType.POSTGRESQL]: 5432,
    [DatabaseType.MYSQL]: 3306,
    [DatabaseType.SQL_SERVER]: 1433,
    [DatabaseType.ORACLE]: 1521,
};

export const ConnectionModal: React.FC<ConnectionModalProps> = ({
    isOpen,
    onClose,
    diagramId,
}) => {
    const [formData, setFormData] = useState<CreateConnectionRequest>({
        diagramId,
        databaseType: DatabaseType.POSTGRESQL,
        name: '',
        host: 'localhost',
        port: DEFAULT_PORTS[DatabaseType.POSTGRESQL],
        databaseName: '',
        username: '',
        password: '',
        environment: 'development',
    });

    const [isTesting, setIsTesting] = useState(false);
    const [testResult, setTestResult] = useState<{
        success: boolean;
        message: string;
    } | null>(null);
    const [errors, setErrors] = useState<Record<string, string>>({});

    const { createConnection, testConnection, connectionError, clearError } =
        useDatabaseConnectionStore();

    // Reset form when modal opens
    useEffect(() => {
        if (isOpen) {
            setFormData({
                diagramId,
                databaseType: DatabaseType.POSTGRESQL,
                name: '',
                host: 'localhost',
                port: DEFAULT_PORTS[DatabaseType.POSTGRESQL],
                databaseName: '',
                username: '',
                password: '',
                environment: 'development',
            });
            setTestResult(null);
            setErrors({});
            clearError();
        }
    }, [isOpen, diagramId, clearError]);

    // Update port when database type changes
    const handleDatabaseTypeChange = (type: DatabaseType) => {
        setFormData((prev) => ({
            ...prev,
            databaseType: type,
            port: DEFAULT_PORTS[type],
        }));
        setTestResult(null);
    };

    const handleInputChange = (
        field: keyof CreateConnectionRequest,
        value: string | number
    ) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
        setTestResult(null);
        if (errors[field]) {
            setErrors((prev) => {
                const newErrors = { ...prev };
                delete newErrors[field];
                return newErrors;
            });
        }
    };

    const validateForm = (): boolean => {
        const newErrors: Record<string, string> = {};

        if (!formData.name.trim()) {
            newErrors.name = 'Connection name is required';
        }
        if (!formData.host.trim()) {
            newErrors.host = 'Host is required';
        }
        if (!formData.databaseName.trim()) {
            newErrors.databaseName = 'Database name is required';
        }
        if (!formData.username.trim()) {
            newErrors.username = 'Username is required';
        }
        if (!formData.password.trim()) {
            newErrors.password = 'Password is required';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleTestConnection = async () => {
        if (!validateForm()) {
            return;
        }

        setIsTesting(true);
        setTestResult(null);

        try {
            const success = await testConnection({
                databaseType: formData.databaseType,
                host: formData.host,
                port: formData.port,
                databaseName: formData.databaseName,
                username: formData.username,
                password: formData.password,
            });

            setTestResult({
                success,
                message: success
                    ? 'Connection successful!'
                    : 'Connection failed. Please check your credentials.',
            });
        } catch (error) {
            setTestResult({
                success: false,
                message:
                    error instanceof Error
                        ? error.message
                        : 'Connection test failed',
            });
        } finally {
            setIsTesting(false);
        }
    };

    const handleSave = async () => {
        if (!validateForm()) {
            return;
        }

        try {
            await createConnection(formData);
            onClose();
        } catch {
            // Error is handled by the store
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[550px]">
                <DialogHeader>
                    <DialogTitle>Add Database Connection</DialogTitle>
                </DialogHeader>

                <div className="space-y-4 py-4">
                    {/* Connection Name */}
                    <div className="space-y-2">
                        <Label htmlFor="name">Connection Name *</Label>
                        <Input
                            id="name"
                            value={formData.name}
                            onChange={(e) =>
                                handleInputChange('name', e.target.value)
                            }
                            placeholder="Production Database"
                            className={errors.name ? 'border-red-500' : ''}
                        />
                        {errors.name && (
                            <p className="text-sm text-red-500">
                                {errors.name}
                            </p>
                        )}
                    </div>

                    {/* Database Type */}
                    <div className="space-y-2">
                        <Label htmlFor="databaseType">Database Type *</Label>
                        <Select
                            value={formData.databaseType}
                            onValueChange={handleDatabaseTypeChange}
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value={DatabaseType.POSTGRESQL}>
                                    PostgreSQL
                                </SelectItem>
                                <SelectItem value={DatabaseType.MYSQL}>
                                    MySQL
                                </SelectItem>
                                <SelectItem value={DatabaseType.SQL_SERVER}>
                                    SQL Server
                                </SelectItem>
                                <SelectItem value={DatabaseType.ORACLE}>
                                    Oracle
                                </SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Host and Port */}
                    <div className="grid grid-cols-3 gap-4">
                        <div className="col-span-2 space-y-2">
                            <Label htmlFor="host">Host *</Label>
                            <Input
                                id="host"
                                value={formData.host}
                                onChange={(e) =>
                                    handleInputChange('host', e.target.value)
                                }
                                placeholder="localhost"
                                className={errors.host ? 'border-red-500' : ''}
                            />
                            {errors.host && (
                                <p className="text-sm text-red-500">
                                    {errors.host}
                                </p>
                            )}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="port">Port *</Label>
                            <Input
                                id="port"
                                type="number"
                                value={formData.port}
                                onChange={(e) =>
                                    handleInputChange(
                                        'port',
                                        parseInt(e.target.value)
                                    )
                                }
                            />
                        </div>
                    </div>

                    {/* Database Name */}
                    <div className="space-y-2">
                        <Label htmlFor="databaseName">Database Name *</Label>
                        <Input
                            id="databaseName"
                            value={formData.databaseName}
                            onChange={(e) =>
                                handleInputChange(
                                    'databaseName',
                                    e.target.value
                                )
                            }
                            placeholder="mydb"
                            className={
                                errors.databaseName ? 'border-red-500' : ''
                            }
                        />
                        {errors.databaseName && (
                            <p className="text-sm text-red-500">
                                {errors.databaseName}
                            </p>
                        )}
                    </div>

                    {/* Username */}
                    <div className="space-y-2">
                        <Label htmlFor="username">Username *</Label>
                        <Input
                            id="username"
                            value={formData.username}
                            onChange={(e) =>
                                handleInputChange('username', e.target.value)
                            }
                            placeholder="dbuser"
                            className={errors.username ? 'border-red-500' : ''}
                        />
                        {errors.username && (
                            <p className="text-sm text-red-500">
                                {errors.username}
                            </p>
                        )}
                    </div>

                    {/* Password */}
                    <div className="space-y-2">
                        <Label htmlFor="password">Password *</Label>
                        <Input
                            id="password"
                            type="password"
                            value={formData.password}
                            onChange={(e) =>
                                handleInputChange('password', e.target.value)
                            }
                            placeholder="••••••••"
                            className={errors.password ? 'border-red-500' : ''}
                        />
                        {errors.password && (
                            <p className="text-sm text-red-500">
                                {errors.password}
                            </p>
                        )}
                    </div>

                    {/* Environment */}
                    <div className="space-y-2">
                        <Label htmlFor="environment">Environment</Label>
                        <Select
                            value={formData.environment}
                            onValueChange={(value) =>
                                handleInputChange('environment', value)
                            }
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="development">
                                    Development
                                </SelectItem>
                                <SelectItem value="staging">Staging</SelectItem>
                                <SelectItem value="production">
                                    Production
                                </SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Test Connection Result */}
                    {testResult && (
                        <Alert
                            variant={
                                testResult.success ? 'default' : 'destructive'
                            }
                        >
                            <div className="flex items-center gap-2">
                                {testResult.success ? (
                                    <Check className="size-4 text-green-500" />
                                ) : (
                                    <X className="size-4 text-red-500" />
                                )}
                                <AlertDescription>
                                    {testResult.message}
                                </AlertDescription>
                            </div>
                        </Alert>
                    )}

                    {/* Connection Error */}
                    {connectionError && (
                        <Alert variant="destructive">
                            <AlertDescription>
                                {connectionError}
                            </AlertDescription>
                        </Alert>
                    )}
                </div>

                <DialogFooter className="gap-2">
                    <Button variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button
                        variant="secondary"
                        onClick={handleTestConnection}
                        disabled={isTesting}
                    >
                        {isTesting ? (
                            <>
                                <Loader2 className="mr-2 size-4 animate-spin" />
                                Testing...
                            </>
                        ) : (
                            'Test Connection'
                        )}
                    </Button>
                    <Button
                        onClick={handleSave}
                        disabled={
                            isTesting ||
                            (testResult !== null && !testResult.success)
                        }
                    >
                        Save Connection
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
