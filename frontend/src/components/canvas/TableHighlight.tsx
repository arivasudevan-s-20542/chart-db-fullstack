import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';

interface TableHighlightProps {
    isHighlighted: boolean;
    highlightColor?: string;
    duration?: number;
}

export const TableHighlight: React.FC<TableHighlightProps> = ({
    isHighlighted,
    highlightColor = '#8B5CF6', // purple-500
    duration = 2000,
}) => {
    const [show, setShow] = useState(false);

    useEffect(() => {
        if (isHighlighted) {
            setShow(true);
            const timer = setTimeout(() => setShow(false), duration);
            return () => clearTimeout(timer);
        }
    }, [isHighlighted, duration]);

    if (!show) return null;

    return (
        <motion.div
            className="absolute inset-0 pointer-events-none"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{
                opacity: [0, 1, 1, 0],
                scale: [0.95, 1.02, 1.02, 1],
            }}
            transition={{
                duration: duration / 1000,
                times: [0, 0.1, 0.9, 1],
            }}
            style={{
                boxShadow: `0 0 0 3px ${highlightColor}`,
                borderRadius: '8px',
            }}
        />
    );
};

interface ElementHighlighterProps {
    highlightedElements: Set<string>;
    onClearHighlight?: () => void;
}

export const ElementHighlighter: React.FC<ElementHighlighterProps> = ({
    highlightedElements,
    onClearHighlight,
}) => {
    useEffect(() => {
        if (highlightedElements.size > 0 && onClearHighlight) {
            const timer = setTimeout(() => {
                onClearHighlight();
            }, 3000);
            return () => clearTimeout(timer);
        }
    }, [highlightedElements, onClearHighlight]);

    return null; // This component only manages state
};

/**
 * Hook to manage table highlighting state
 */
export const useTableHighlight = () => {
    const [highlightedTables, setHighlightedTables] = useState<Set<string>>(new Set());

    const highlightTable = (tableId: string) => {
        setHighlightedTables(new Set([tableId]));
    };

    const highlightMultipleTables = (tableIds: string[]) => {
        setHighlightedTables(new Set(tableIds));
    };

    const clearHighlights = () => {
        setHighlightedTables(new Set());
    };

    const isHighlighted = (tableId: string) => {
        return highlightedTables.has(tableId);
    };

    return {
        highlightedTables,
        highlightTable,
        highlightMultipleTables,
        clearHighlights,
        isHighlighted,
    };
};

/**
 * Hook to pan and zoom to a specific table on the canvas
 */
export const useCanvasPanZoom = (reactFlowInstance: any) => {
    const panToTable = (tableId: string, duration: number = 500) => {
        if (!reactFlowInstance) return;

        const node = reactFlowInstance.getNode(tableId);
        if (!node) return;

        reactFlowInstance.fitView({
            nodes: [node],
            duration,
            padding: 0.3,
        });
    };

    const panToTables = (tableIds: string[], duration: number = 500) => {
        if (!reactFlowInstance || tableIds.length === 0) return;

        const nodes = tableIds.map((id) => reactFlowInstance.getNode(id)).filter(Boolean);
        if (nodes.length === 0) return;

        reactFlowInstance.fitView({
            nodes,
            duration,
            padding: 0.2,
        });
    };

    return {
        panToTable,
        panToTables,
    };
};
