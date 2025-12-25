import React, { useCallback, useState } from 'react';
import { Button } from '@/components/button/button';
import { CodeSnippet } from '@/components/code-snippet/code-snippet';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText, Upload } from 'lucide-react';
import { Label } from '@/components/label/label';
import { Textarea } from '@/components/textarea/textarea';

export const DataDictionaryViewerPage: React.FC = () => {
    const navigate = useNavigate();
    const [xmlContent, setXmlContent] = useState<string>('');
    const [fileName, setFileName] = useState<string>('');

    const handleFileUpload = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.[0];
            if (!file) return;

            setFileName(file.name);

            const reader = new FileReader();
            reader.onload = (e) => {
                const content = e.target?.result as string;
                setXmlContent(content);
            };
            reader.readAsText(file);
        },
        []
    );

    const handlePasteContent = useCallback(
        (event: React.ChangeEvent<HTMLTextAreaElement>) => {
            setXmlContent(event.target.value);
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
                        <ArrowLeft className="size-5" />
                    </Button>
                    <div className="flex items-center gap-2">
                        <FileText className="size-6 text-primary" />
                        <h1 className="text-2xl font-bold">
                            Data Dictionary Viewer
                        </h1>
                    </div>
                </div>
                <div className="flex gap-2">
                    <label htmlFor="xml-file-upload">
                        <Button variant="outline" asChild>
                            <span className="cursor-pointer">
                                <Upload className="mr-2 size-4" />
                                Upload XML File
                            </span>
                        </Button>
                    </label>
                    <input
                        id="xml-file-upload"
                        type="file"
                        accept=".xml,.txt"
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
                            Data Dictionary XML Input
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            Paste your Data Dictionary XML content or upload a
                            file
                        </p>
                    </div>
                    <Textarea
                        placeholder="Paste your Data Dictionary XML content here...

Example:
<?xml version='1.0' encoding='UTF-8'?>
<DataDictionary>
  <Table name='users'>
    <Column name='id' type='integer' isPrimaryKey='true' />
    <Column name='username' type='varchar' />
    <Column name='created_at' type='timestamp' />
  </Table>
  <Table name='posts'>
    <Column name='id' type='integer' isPrimaryKey='true' />
    <Column name='user_id' type='integer' />
  </Table>
</DataDictionary>"
                        className="flex-1 font-mono text-sm"
                        value={xmlContent}
                        onChange={handlePasteContent}
                    />
                </div>

                {/* Right Panel - Viewer */}
                <div className="flex w-1/2 flex-col p-6">
                    <div className="mb-4">
                        <Label className="text-lg font-semibold">
                            {fileName || 'Data Dictionary Preview'}
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            Syntax-highlighted XML view
                        </p>
                    </div>
                    {xmlContent ? (
                        <div className="flex-1 overflow-hidden rounded-lg border">
                            <CodeSnippet
                                className="size-full"
                                code={xmlContent}
                                autoScroll={false}
                                isComplete={true}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-1 items-center justify-center rounded-lg border border-dashed">
                            <div className="flex flex-col items-center gap-2 text-muted-foreground">
                                <FileText className="size-12" />
                                <p className="text-sm">
                                    Upload a Data Dictionary XML file or paste
                                    content to view
                                </p>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Footer */}
            <div className="border-t px-6 py-3">
                <p className="text-sm text-muted-foreground">
                    Data Dictionary XML format provides a structured
                    representation of your database schema with detailed
                    metadata about tables, columns, relationships, and
                    constraints.
                </p>
            </div>
        </div>
    );
};
