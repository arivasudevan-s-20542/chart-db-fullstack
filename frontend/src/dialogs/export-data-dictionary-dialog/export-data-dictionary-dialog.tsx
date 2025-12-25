import { Button } from '@/components/button/button';
import { CodeSnippet } from '@/components/code-snippet/code-snippet';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogInternalContent,
    DialogTitle,
} from '@/components/dialog/dialog';
import { Label } from '@/components/label/label';
import { Spinner } from '@/components/spinner/spinner';
import { useChartDB } from '@/hooks/use-chartdb';
import { useDialog } from '@/hooks/use-dialog';
import { exportDataDictionary } from '@/lib/data/xml-export/export-data-dictionary';
import { Annoyed, Download, FileText } from 'lucide-react';
import React, { useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { BaseDialogProps } from '../common/base-dialog-props';
import type { Diagram } from '@/lib/domain/diagram';
import { useDiagramFilter } from '@/context/diagram-filter-context/use-diagram-filter';
import { filterTable } from '@/lib/domain/diagram-filter/filter';
import { defaultSchemas } from '@/lib/data/default-schemas';

export interface ExportDataDictionaryDialogProps extends BaseDialogProps {}

export const ExportDataDictionaryDialog: React.FC<
    ExportDataDictionaryDialogProps
> = ({ dialog }) => {
    const { closeExportDataDictionaryDialog } = useDialog();
    const { currentDiagram } = useChartDB();
    const { filter } = useDiagramFilter();
    useTranslation();
    const [xmlContent, setXmlContent] = React.useState<string>();
    const [error, setError] = React.useState<boolean>(false);
    const [isLoading, setIsLoading] = React.useState<boolean>(false);

    const generateXML = useCallback(async () => {
        const filteredDiagram: Diagram = {
            ...currentDiagram,
            tables: currentDiagram.tables?.filter((table) =>
                filterTable({
                    table: {
                        id: table.id,
                        schema: table.schema,
                    },
                    filter,
                    options: {
                        defaultSchema:
                            defaultSchemas[currentDiagram.databaseType],
                    },
                })
            ),
        };

        return Promise.resolve(
            exportDataDictionary({
                diagram: filteredDiagram,
            })
        );
    }, [currentDiagram, filter]);

    useEffect(() => {
        if (!dialog.open) {
            return;
        }

        setXmlContent(undefined);
        setError(false);

        const fetchXML = async () => {
            try {
                setIsLoading(true);
                const xml = await generateXML();
                setXmlContent(xml);
                setIsLoading(false);
            } catch (err) {
                console.error('Failed to generate data dictionary:', err);
                setError(true);
                setIsLoading(false);
            }
        };

        fetchXML();
    }, [dialog.open, generateXML]);

    const handleDownload = useCallback(() => {
        if (!xmlContent) return;

        const blob = new Blob([xmlContent], {
            type: 'application/xml;charset=utf-8',
        });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${currentDiagram.name || 'data-dictionary'}.xml`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }, [xmlContent, currentDiagram.name]);

    const renderError = useCallback(
        () => (
            <div className="flex flex-col gap-2">
                <div className="flex flex-col items-center justify-center gap-1 text-sm">
                    <Annoyed className="size-10" />
                    <Label className="text-sm">
                        Failed to generate data dictionary XML
                    </Label>
                    <div>Please try again or report this issue.</div>
                </div>
            </div>
        ),
        []
    );

    const renderLoader = useCallback(
        () => (
            <div className="flex flex-col gap-2">
                <Spinner />
                <div className="flex items-center justify-center gap-1">
                    <FileText className="h-5" />
                    <Label className="text-lg">
                        Generating Data Dictionary XML...
                    </Label>
                </div>
                <div className="flex items-center justify-center gap-1">
                    <Label className="text-sm">
                        This may take a moment for large diagrams
                    </Label>
                </div>
            </div>
        ),
        []
    );

    return (
        <Dialog
            {...dialog}
            onOpenChange={(open) => {
                if (!open) {
                    closeExportDataDictionaryDialog();
                }
            }}
        >
            <DialogContent
                className="flex max-h-screen flex-col overflow-y-auto xl:min-w-[75vw]"
                showClose
            >
                <DialogHeader>
                    <DialogTitle>Export Data Dictionary (XML)</DialogTitle>
                    <DialogDescription>
                        Export your database schema as a detailed XML data
                        dictionary format
                    </DialogDescription>
                </DialogHeader>
                <DialogInternalContent>
                    <div className="flex flex-1 items-center justify-center">
                        {error ? (
                            renderError()
                        ) : xmlContent === undefined ? (
                            renderLoader()
                        ) : xmlContent.length === 0 ? (
                            renderError()
                        ) : (
                            <CodeSnippet
                                className="h-96 w-full"
                                code={xmlContent}
                                autoScroll={false}
                                isComplete={!isLoading}
                            />
                        )}
                    </div>
                </DialogInternalContent>
                <DialogFooter className="flex !justify-between gap-2">
                    <Button
                        type="button"
                        variant="default"
                        onClick={handleDownload}
                        disabled={!xmlContent}
                    >
                        <Download className="mr-2 size-4" />
                        Download XML
                    </Button>
                    <DialogClose asChild>
                        <Button type="button" variant="secondary">
                            Close
                        </Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
