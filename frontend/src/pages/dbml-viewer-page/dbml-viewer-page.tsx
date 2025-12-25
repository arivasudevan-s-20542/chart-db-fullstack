import React, { useCallback, useState } from 'react';
import { Button } from '@/components/button/button';
import { CodeSnippet } from '@/components/code-snippet/code-snippet';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText, Upload } from 'lucide-react';
import { Label } from '@/components/label/label';
import { Textarea } from '@/components/textarea/textarea';

export const DBMLViewerPage: React.FC = () => {
    const navigate = useNavigate();
    const [dbmlContent, setDbmlContent] = useState<string>('');
    const [fileName, setFileName] = useState<string>('');

    const handleFileUpload = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.[0];
            if (!file) return;

            setFileName(file.name);

            const reader = new FileReader();
            reader.onload = (e) => {
                const content = e.target?.result as string;
                setDbmlContent(content);
            };
            reader.readAsText(file);
        },
        []
    );

    const handlePasteContent = useCallback(
        (event: React.ChangeEvent<HTMLTextAreaElement>) => {
            setDbmlContent(event.target.value);
            setFileName('Pasted Content');
        },
        []
    );

    const handleBackToEditor = useCallback(() => {
        navigate('/editor');
    }, [navigate]);

    return (
        <div className="flex h-screen flex-col bg-background">
            {/* Header */}
            <div className="flex items-center justify-between border-b px-6 py-4">
                <div className="flex items-center gap-4">
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={handleBackToEditor}
                    >
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <div className="flex items-center gap-2">
                        <FileText className="h-6 w-6 text-primary" />
                        <h1 className="text-2xl font-bold">DBML Viewer</h1>
                    </div>
                </div>
                <div className="flex gap-2">
                    <label htmlFor="dbml-file-upload">
                        <Button variant="outline" asChild>
                            <span className="cursor-pointer">
                                <Upload className="mr-2 h-4 w-4" />
                                Upload DBML File
                            </span>
                        </Button>
                    </label>
                    <input
                        id="dbml-file-upload"
                        type="file"
                        accept=".dbml,.txt"
                        className="hidden"
                        onChange={handleFileUpload}
                    />
                </div>
            </div>

            {/* Main Content Area */}
            <div className="flex flex-1 overflow-hidden">
                {/* Left Panel - Input */}
                <div className="flex w-1/2 flex-col border-r p-6">
                    <div className="mb-4">
                        <Label className="text-lg font-semibold">
                            DBML Input
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            Paste your DBML content or upload a file
                        </p>
                    </div>
                    <Textarea
                        placeholder="Paste your DBML content here...

Example:
Table users {
  id integer [primary key]
  username varchar
  created_at timestamp
}

Table posts {
  id integer [primary key]
  user_id integer [ref: > users.id]
  title varchar
  content text
}"
                        className="flex-1 font-mono text-sm"
                        value={dbmlContent}
                        onChange={handlePasteContent}
                    />
                </div>

                {/* Right Panel - Viewer */}
                <div className="flex w-1/2 flex-col p-6">
                    <div className="mb-4">
                        <Label className="text-lg font-semibold">
                            {fileName || 'DBML Preview'}
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            Syntax-highlighted DBML view
                        </p>
                    </div>
                    {dbmlContent ? (
                        <div className="flex-1 overflow-hidden rounded-lg border">
                            <CodeSnippet
                                className="h-full w-full"
                                code={dbmlContent}
                                autoScroll={false}
                                isComplete={true}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-1 items-center justify-center rounded-lg border border-dashed">
                            <div className="flex flex-col items-center gap-2 text-muted-foreground">
                                <FileText className="h-12 w-12" />
                                <p className="text-sm">
                                    Upload a DBML file or paste content to view
                                </p>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Footer */}
            <div className="border-t px-6 py-3">
                <p className="text-sm text-muted-foreground">
                    DBML (Database Markup Language) is a simple, readable DSL
                    for defining database schemas. Learn more at{' '}
                    <a
                        href="https://dbml.dbdiagram.io/"
                        target="_blank"
                        rel="noreferrer"
                        className="text-primary hover:underline"
                    >
                        dbml.dbdiagram.io
                    </a>
                </p>
            </div>
        </div>
    );
};
