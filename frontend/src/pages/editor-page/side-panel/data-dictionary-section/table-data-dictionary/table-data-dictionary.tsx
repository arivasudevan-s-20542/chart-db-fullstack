import React, {
    useMemo,
    useState,
    useEffect,
    useCallback,
    useRef,
} from 'react';
import { useChartDB } from '@/hooks/use-chartdb';
import { useTheme } from '@/hooks/use-theme';
import { CodeSnippet } from '@/components/code-snippet/code-snippet';
import type { EffectiveTheme } from '@/context/theme-context/theme-context';
import { useToast } from '@/components/toast/use-toast';
import { Download } from 'lucide-react';
import { exportDataDictionary } from '@/lib/data/xml-export/export-data-dictionary';

export interface TableDataDictionaryProps {}

const getEditorTheme = (theme: EffectiveTheme) => {
    return theme === 'dark' ? 'vs-dark' : 'vs';
};

export const TableDataDictionary: React.FC<TableDataDictionaryProps> = () => {
    const { currentDiagram, databaseType } = useChartDB();
    const { effectiveTheme } = useTheme();
    const { toast } = useToast();
    const [isLoading, setIsLoading] = useState(true);
    const [xmlContent, setXmlContent] = useState('');

    useEffect(() => {
        const generateXML = async () => {
            setIsLoading(true);

            try {
                const result = exportDataDictionary({
                    diagram: currentDiagram,
                    databaseType,
                });

                setXmlContent(result);
            } catch (error) {
                toast({
                    title: 'Data Dictionary Export Error',
                    description: `Could not generate XML: ${error instanceof Error ? error.message : String(error)}`,
                    variant: 'destructive',
                });
                setXmlContent('');
            }

            setIsLoading(false);
        };

        setTimeout(() => generateXML(), 0);
    }, [currentDiagram, databaseType, toast]);

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

        toast({
            title: 'Download Started',
            description: 'Your Data Dictionary XML file is being downloaded.',
        });
    }, [xmlContent, currentDiagram.name, toast]);

    return (
        <CodeSnippet
            code={xmlContent}
            loading={isLoading}
            actionsTooltipSide="right"
            className="my-0.5"
            allowCopy={true}
            actions={[
                {
                    label: 'Download XML',
                    icon: Download,
                    onClick: handleDownload,
                    className:
                        'h-7 items-center gap-1.5 rounded-md border border-green-200 bg-green-50 px-2.5 py-1.5 text-xs font-medium text-green-600 shadow-sm hover:bg-green-100 dark:border-green-800 dark:bg-green-800 dark:text-green-200 dark:hover:bg-green-700',
                },
            ]}
            editorProps={{
                height: '100%',
                defaultLanguage: 'xml',
                theme: getEditorTheme(effectiveTheme),
                options: {
                    wordWrap: 'off',
                    mouseWheelZoom: false,
                    readOnly: true,
                    autoClosingBrackets: 'always',
                    autoClosingQuotes: 'always',
                    autoSurround: 'languageDefined',
                },
            }}
        />
    );
};
