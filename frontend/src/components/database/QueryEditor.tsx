import React, { useState, useRef, useEffect } from 'react';
import type { Monaco } from '@monaco-editor/react';
import Editor from '@monaco-editor/react';
import type * as monaco from 'monaco-editor';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/card/card';
import { Button } from '@/components/button/button';
import { Badge } from '@/components/badge/badge';
import { Alert, AlertDescription } from '@/components/alert/alert';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/tabs/tabs';
import { useDatabaseConnectionStore } from '@/stores/database-connection.store';
import type { DatabaseConnection } from '@/types/database.types';
import {
    Play,
    Save,
    History,
    Clock,
    Download,
    AlertCircle,
} from 'lucide-react';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/table/table';

interface QueryEditorProps {
    connection: DatabaseConnection;
}

export const QueryEditor: React.FC<QueryEditorProps> = ({ connection }) => {
    const [sql, setSql] = useState(
        '-- Write your SQL query here\nSELECT * FROM information_schema.tables LIMIT 10;'
    );
    const [selectedTab, setSelectedTab] = useState<'results' | 'history'>(
        'results'
    );
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

    const {
        queryResults,
        isExecutingQuery,
        queryError,
        queryHistory,
        isLoadingHistory,
        executeQuery,
        loadQueryHistory,
    } = useDatabaseConnectionStore();

    useEffect(() => {
        if (connection) {
            loadQueryHistory(connection.id);
        }
    }, [connection, loadQueryHistory]);

    const handleEditorDidMount = (
        editor: monaco.editor.IStandaloneCodeEditor,
        monaco: Monaco
    ) => {
        editorRef.current = editor;

        // Add keyboard shortcut for running query (Cmd/Ctrl + Enter)
        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
            handleExecuteQuery();
        });

        // Configure SQL language features
        monaco.languages.setLanguageConfiguration('sql', {
            comments: {
                lineComment: '--',
                blockComment: ['/*', '*/'],
            },
            brackets: [
                ['(', ')'],
                ['[', ']'],
            ],
            autoClosingPairs: [
                { open: '(', close: ')' },
                { open: '[', close: ']' },
                { open: "'", close: "'" },
                { open: '"', close: '"' },
            ],
        });
    };

    const handleExecuteQuery = async () => {
        if (!sql.trim()) {
            return;
        }

        await executeQuery(connection.id, {
            sql: sql.trim(),
            maxRows: 1000,
        });

        // Switch to results tab
        setSelectedTab('results');
    };

    const handleLoadHistoryQuery = (historySql: string) => {
        setSql(historySql);
    };

    const handleExportCSV = () => {
        if (!queryResults) return;

        // Create CSV content
        const headers = queryResults.columns.map((col) => col.name).join(',');
        const rows = queryResults.rows.map((row) =>
            queryResults.columns
                .map((col) => JSON.stringify(row[col.name] ?? ''))
                .join(',')
        );
        const csv = [headers, ...rows].join('\n');

        // Download
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `query_results_${Date.now()}.csv`;
        a.click();
        URL.revokeObjectURL(url);
    };

    const formatExecutionTime = (ms: number) => {
        if (ms < 1000) {
            return `${ms}ms`;
        }
        return `${(ms / 1000).toFixed(2)}s`;
    };

    return (
        <div className="flex h-full flex-col gap-4">
            {/* Connection Info */}
            <Card>
                <CardHeader className="py-3">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <CardTitle className="text-base">
                                {connection.name}
                            </CardTitle>
                            <Badge variant="outline" className="text-xs">
                                {connection.databaseName}
                            </Badge>
                        </div>
                        <div className="flex items-center gap-2">
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => loadQueryHistory(connection.id)}
                            >
                                <History className="size-4" />
                            </Button>
                        </div>
                    </div>
                </CardHeader>
            </Card>

            {/* SQL Editor */}
            <Card className="flex flex-1 flex-col">
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <CardTitle>SQL Query Editor</CardTitle>
                        <div className="flex items-center gap-2">
                            <Button variant="outline" size="sm" disabled>
                                <Save className="mr-2 size-4" />
                                Save Query
                            </Button>
                            <Button
                                onClick={handleExecuteQuery}
                                disabled={isExecutingQuery || !sql.trim()}
                                size="sm"
                            >
                                <Play className="mr-2 size-4" />
                                {isExecutingQuery ? 'Running...' : 'Run Query'}
                            </Button>
                        </div>
                    </div>
                    <CardDescription className="text-xs">
                        Press Cmd/Ctrl + Enter to execute • Maximum 1000 rows
                    </CardDescription>
                </CardHeader>
                <CardContent className="flex-1 p-0">
                    <Editor
                        height="300px"
                        defaultLanguage="sql"
                        value={sql}
                        onChange={(value) => setSql(value || '')}
                        onMount={handleEditorDidMount}
                        theme="vs-dark"
                        options={{
                            minimap: { enabled: false },
                            fontSize: 14,
                            lineNumbers: 'on',
                            scrollBeyondLastLine: false,
                            automaticLayout: true,
                            tabSize: 2,
                            wordWrap: 'on',
                        }}
                    />
                </CardContent>
            </Card>

            {/* Results/History Tabs */}
            <Card className="flex flex-1 flex-col overflow-hidden">
                <Tabs
                    value={selectedTab}
                    onValueChange={(v) => setSelectedTab(v as any)}
                    className="flex flex-1 flex-col"
                >
                    <CardHeader className="pb-3">
                        <TabsList>
                            <TabsTrigger value="results">
                                Query Results
                            </TabsTrigger>
                            <TabsTrigger value="history">
                                History
                                {queryHistory.length > 0 && (
                                    <Badge
                                        variant="secondary"
                                        className="ml-2 text-xs"
                                    >
                                        {queryHistory.length}
                                    </Badge>
                                )}
                            </TabsTrigger>
                        </TabsList>
                    </CardHeader>

                    <CardContent className="flex-1 overflow-auto">
                        {/* Query Results Tab */}
                        <TabsContent value="results" className="mt-0 h-full">
                            {queryError && (
                                <Alert variant="destructive">
                                    <AlertCircle className="size-4" />
                                    <AlertDescription>
                                        {queryError}
                                    </AlertDescription>
                                </Alert>
                            )}

                            {queryResults && (
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-4 text-sm text-gray-500">
                                            <span>
                                                {queryResults.rowCount} row
                                                {queryResults.rowCount !== 1
                                                    ? 's'
                                                    : ''}{' '}
                                                {queryResults.truncated &&
                                                    '(truncated)'}
                                            </span>
                                            <span className="flex items-center gap-1">
                                                <Clock className="size-3" />
                                                {formatExecutionTime(
                                                    queryResults.executionTime
                                                )}
                                            </span>
                                        </div>
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={handleExportCSV}
                                        >
                                            <Download className="mr-2 size-4" />
                                            Export CSV
                                        </Button>
                                    </div>

                                    <div className="max-h-[400px] overflow-auto rounded-lg border">
                                        <Table>
                                            <TableHeader>
                                                <TableRow>
                                                    {queryResults.columns.map(
                                                        (col) => (
                                                            <TableHead
                                                                key={col.name}
                                                                className="font-semibold"
                                                            >
                                                                {col.name}
                                                                <span className="ml-2 text-xs font-normal text-gray-400">
                                                                    {col.type}
                                                                </span>
                                                            </TableHead>
                                                        )
                                                    )}
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {queryResults.rows.map(
                                                    (row, idx) => (
                                                        <TableRow key={idx}>
                                                            {queryResults.columns.map(
                                                                (col) => (
                                                                    <TableCell
                                                                        key={
                                                                            col.name
                                                                        }
                                                                        className="font-mono text-sm"
                                                                    >
                                                                        {row[
                                                                            col
                                                                                .name
                                                                        ] ===
                                                                        null ? (
                                                                            <span className="italic text-gray-400">
                                                                                null
                                                                            </span>
                                                                        ) : (
                                                                            String(
                                                                                row[
                                                                                    col
                                                                                        .name
                                                                                ]
                                                                            )
                                                                        )}
                                                                    </TableCell>
                                                                )
                                                            )}
                                                        </TableRow>
                                                    )
                                                )}
                                            </TableBody>
                                        </Table>
                                    </div>
                                </div>
                            )}

                            {!queryResults &&
                                !queryError &&
                                !isExecutingQuery && (
                                    <div className="py-12 text-center text-gray-500">
                                        <Play className="mx-auto mb-4 size-12 text-gray-300" />
                                        <p>Run a query to see results</p>
                                    </div>
                                )}

                            {isExecutingQuery && (
                                <div className="py-12 text-center text-gray-500">
                                    <div className="mx-auto mb-4 size-8 animate-spin rounded-full border-4 border-gray-300 border-t-blue-500"></div>
                                    <p>Executing query...</p>
                                </div>
                            )}
                        </TabsContent>

                        {/* Query History Tab */}
                        <TabsContent value="history" className="mt-0">
                            {isLoadingHistory ? (
                                <div className="py-12 text-center text-gray-500">
                                    Loading history...
                                </div>
                            ) : queryHistory.length === 0 ? (
                                <div className="py-12 text-center text-gray-500">
                                    <History className="mx-auto mb-4 size-12 text-gray-300" />
                                    <p>No query history yet</p>
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {queryHistory.map((item) => (
                                        <div
                                            key={item.id}
                                            className="cursor-pointer rounded-lg border p-3 hover:bg-gray-50 dark:hover:bg-gray-800"
                                            onClick={() =>
                                                handleLoadHistoryQuery(item.sql)
                                            }
                                        >
                                            <div className="flex items-start justify-between gap-4">
                                                <pre className="flex-1 overflow-x-auto whitespace-pre-wrap font-mono text-sm">
                                                    {item.sql}
                                                </pre>
                                                <Badge
                                                    variant={
                                                        item.status ===
                                                        'SUCCESS'
                                                            ? 'default'
                                                            : 'destructive'
                                                    }
                                                    className="shrink-0 text-xs"
                                                >
                                                    {item.status}
                                                </Badge>
                                            </div>
                                            <div className="mt-2 flex items-center gap-4 text-xs text-gray-500">
                                                <span>
                                                    {item.rowCount} row
                                                    {item.rowCount !== 1
                                                        ? 's'
                                                        : ''}
                                                </span>
                                                <span>
                                                    {formatExecutionTime(
                                                        item.executionTime
                                                    )}
                                                </span>
                                                <span>
                                                    {new Date(
                                                        item.executedAt
                                                    ).toLocaleString()}
                                                </span>
                                            </div>
                                            {item.errorMessage && (
                                                <p className="mt-2 text-xs text-red-500">
                                                    {item.errorMessage}
                                                </p>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </TabsContent>
                    </CardContent>
                </Tabs>
            </Card>
        </div>
    );
};
