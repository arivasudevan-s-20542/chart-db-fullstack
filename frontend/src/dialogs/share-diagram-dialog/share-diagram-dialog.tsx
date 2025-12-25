import React, { useCallback, useEffect, useState } from 'react';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
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
import { useChartDB } from '@/hooks/use-chartdb';
import { useToast } from '@/components/toast/use-toast';
import { diagramsApi } from '@/services/api/diagrams.api';
import {
    Trash2,
    UserPlus,
    Users,
    Loader2,
    Crown,
    Copy,
    Check,
    X,
} from 'lucide-react';
import { Avatar, AvatarFallback } from '@/components/avatar/avatar';
import { Badge } from '@/components/badge/badge';
import type { BaseDialogProps } from '../common/base-dialog-props';

type Permission = 'VIEW' | 'EDIT' | 'ADMIN';

interface Collaborator {
    userId: string;
    email: string;
    name?: string;
    permission: Permission;
    isOwner?: boolean;
}

export interface ShareDiagramDialogProps extends BaseDialogProps {}

export const ShareDiagramDialog: React.FC<ShareDiagramDialogProps> = ({
    dialog,
}) => {
    const { currentDiagram, permissionLevel } = useChartDB();
    const { toast } = useToast();
    const [email, setEmail] = useState('');
    const [permission, setPermission] = useState<Permission>('EDIT');
    const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSharing, setIsSharing] = useState(false);
    const [copied, setCopied] = useState(false);

    const diagramId = currentDiagram?.id;

    // Only OWNER can manage permissions
    const canManagePermissions = permissionLevel === 'OWNER';

    // Load collaborators when dialog opens
    useEffect(() => {
        if (dialog.open && diagramId) {
            loadCollaborators();
        }
    }, [dialog.open, diagramId]);

    const loadCollaborators = async () => {
        if (!diagramId) return;
        setIsLoading(true);
        try {
            const result = await diagramsApi.getCollaborators(diagramId);
            setCollaborators(result);
        } catch (error) {
            console.error('Failed to load collaborators:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleShare = useCallback(async () => {
        if (!diagramId || !email.trim()) return;

        setIsSharing(true);
        try {
            await diagramsApi.shareDiagram(diagramId, email.trim(), permission);
            toast({
                title: 'Diagram shared',
                description: `Successfully shared with ${email}`,
            });
            setEmail('');
            loadCollaborators();
        } catch (error: any) {
            toast({
                title: 'Failed to share',
                description: error.message || 'Could not share the diagram',
                variant: 'destructive',
            });
        } finally {
            setIsSharing(false);
        }
    }, [diagramId, email, permission, toast]);

    const handleRemove = useCallback(
        async (userId: string, userEmail: string) => {
            if (!diagramId) return;

            try {
                await diagramsApi.unshareDiagram(diagramId, userId);
                toast({
                    title: 'Access removed',
                    description: `Removed access for ${userEmail}`,
                });
                loadCollaborators();
            } catch (error: any) {
                toast({
                    title: 'Failed to remove',
                    description: error.message || 'Could not remove access',
                    variant: 'destructive',
                });
            }
        },
        [diagramId, toast]
    );

    const handlePermissionChange = useCallback(
        async (userId: string, newPermission: Permission) => {
            if (!diagramId) return;

            try {
                await diagramsApi.updatePermission(
                    diagramId,
                    userId,
                    newPermission
                );
                toast({
                    title: 'Permission updated',
                    description: `Permission changed to ${getPermissionLabel(newPermission)}`,
                });
                loadCollaborators();
            } catch (error: any) {
                toast({
                    title: 'Failed to update permission',
                    description: error.message || 'Could not update permission',
                    variant: 'destructive',
                });
            }
        },
        [diagramId, toast]
    );

    const handleClose = useCallback(() => {
        setEmail('');
        setPermission('EDIT');
        dialog.onOpenChange?.(false);
    }, [dialog]);

    const handleCopyLink = useCallback(() => {
        const url = `${window.location.origin}/diagrams/${diagramId}`;
        navigator.clipboard.writeText(url);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
        toast({
            title: 'Link copied',
            description: 'Diagram link copied to clipboard',
        });
    }, [diagramId, toast]);

    const getInitials = (name?: string, email?: string) => {
        if (name) {
            return name
                .split(' ')
                .map((n) => n[0])
                .join('')
                .toUpperCase()
                .slice(0, 2);
        }
        return email?.slice(0, 2).toUpperCase() || '??';
    };

    const getPermissionLabel = (perm: Permission) => {
        switch (perm) {
            case 'VIEW':
                return 'Can view';
            case 'EDIT':
                return 'Can edit';
            case 'ADMIN':
                return 'Admin';
            default:
                return perm;
        }
    };

    return (
        <Dialog
            {...dialog}
            onOpenChange={(open) => {
                if (!open) {
                    setEmail('');
                    setPermission('EDIT');
                }
                dialog.onOpenChange?.(open);
            }}
        >
            <DialogContent className="sm:max-w-md">
                <DialogClose
                    className="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground"
                    onClick={handleClose}
                >
                    <X className="size-4" />
                    <span className="sr-only">Close</span>
                </DialogClose>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Users className="size-5" />
                        Share Diagram
                    </DialogTitle>
                    <DialogDescription>
                        Invite others to collaborate on "
                        {currentDiagram?.name || 'this diagram'}"
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                    {/* Share by email - only for owners */}
                    {canManagePermissions && (
                        <div className="space-y-2">
                            <Label>Invite by email</Label>
                            <div className="flex gap-2">
                                <Input
                                    type="email"
                                    placeholder="Enter email address"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter') {
                                            handleShare();
                                        }
                                    }}
                                    className="flex-1"
                                />
                                <Select
                                    value={permission}
                                    onValueChange={(v) =>
                                        setPermission(v as Permission)
                                    }
                                >
                                    <SelectTrigger className="w-28">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="VIEW">
                                            View
                                        </SelectItem>
                                        <SelectItem value="EDIT">
                                            Edit
                                        </SelectItem>
                                        <SelectItem value="ADMIN">
                                            Admin
                                        </SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <Button
                                onClick={handleShare}
                                disabled={!email.trim() || isSharing}
                                className="w-full"
                            >
                                {isSharing ? (
                                    <Loader2 className="mr-2 size-4 animate-spin" />
                                ) : (
                                    <UserPlus className="mr-2 size-4" />
                                )}
                                Share
                            </Button>
                        </div>
                    )}

                    {/* Copy link */}
                    <div className="flex items-center gap-2 rounded-md border p-2">
                        <Input
                            readOnly
                            value={`${window.location.origin}/diagrams/${diagramId}`}
                            className="flex-1 border-0 bg-transparent text-sm"
                        />
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={handleCopyLink}
                        >
                            {copied ? (
                                <Check className="size-4 text-green-500" />
                            ) : (
                                <Copy className="size-4" />
                            )}
                        </Button>
                    </div>

                    {/* Collaborators list */}
                    <div className="space-y-2">
                        <Label>People with access</Label>
                        <div className="max-h-48 space-y-2 overflow-y-auto">
                            {isLoading ? (
                                <div className="flex items-center justify-center py-4">
                                    <Loader2 className="size-6 animate-spin text-muted-foreground" />
                                </div>
                            ) : collaborators.length === 0 ? (
                                <p className="py-4 text-center text-sm text-muted-foreground">
                                    Only you have access to this diagram
                                </p>
                            ) : (
                                collaborators.map((collab) => (
                                    <div
                                        key={collab.userId}
                                        className="flex items-center justify-between rounded-md border p-2"
                                    >
                                        <div className="flex items-center gap-2">
                                            <Avatar className="size-8">
                                                <AvatarFallback className="text-xs">
                                                    {getInitials(
                                                        collab.name,
                                                        collab.email
                                                    )}
                                                </AvatarFallback>
                                            </Avatar>
                                            <div>
                                                <p className="text-sm font-medium">
                                                    {collab.name ||
                                                        collab.email}
                                                </p>
                                                {collab.name && (
                                                    <p className="text-xs text-muted-foreground">
                                                        {collab.email}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            {collab.isOwner ? (
                                                <Badge
                                                    variant="secondary"
                                                    className="gap-1"
                                                >
                                                    <Crown className="size-3" />
                                                    Owner
                                                </Badge>
                                            ) : canManagePermissions ? (
                                                <>
                                                    <Select
                                                        value={
                                                            collab.permission
                                                        }
                                                        onValueChange={(v) =>
                                                            handlePermissionChange(
                                                                collab.userId,
                                                                v as Permission
                                                            )
                                                        }
                                                    >
                                                        <SelectTrigger className="h-8 w-24">
                                                            <SelectValue />
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectItem value="VIEW">
                                                                View
                                                            </SelectItem>
                                                            <SelectItem value="EDIT">
                                                                Edit
                                                            </SelectItem>
                                                            <SelectItem value="ADMIN">
                                                                Admin
                                                            </SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                    <Button
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-destructive hover:text-destructive"
                                                        onClick={() =>
                                                            handleRemove(
                                                                collab.userId,
                                                                collab.email
                                                            )
                                                        }
                                                    >
                                                        <Trash2 className="size-4" />
                                                    </Button>
                                                </>
                                            ) : (
                                                <Badge variant="outline">
                                                    {getPermissionLabel(
                                                        collab.permission
                                                    )}
                                                </Badge>
                                            )}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={handleClose}>
                        Done
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ShareDiagramDialog;
