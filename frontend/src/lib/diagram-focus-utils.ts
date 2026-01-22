/**
 * Focus and highlight utilities for AI actions
 * Makes elements visually stand out when clicked from AI action cards
 */

import type { Node, Edge } from '@xyflow/react';

export interface FocusOptions {
    zoom?: number;
    duration?: number;
    padding?: number;
    highlight?: boolean;
    highlightDuration?: number;
}

const DEFAULT_OPTIONS: FocusOptions = {
    zoom: 1.5,
    duration: 500,
    padding: 100,
    highlight: true,
    highlightDuration: 2000,
};

/**
 * Focus on a table node in the diagram
 */
export function focusOnTable(
    tableId: string,
    nodes: Node[],
    reactFlowInstance: any,
    options: FocusOptions = {}
) {
    const opts = { ...DEFAULT_OPTIONS, ...options };
    const node = nodes.find((n) => n.id === tableId);

    if (!node || !reactFlowInstance) {
        console.warn('Cannot focus: node or reactFlowInstance not found');
        return;
    }

    // Calculate center position
    const x = node.position.x + (node.width || 0) / 2;
    const y = node.position.y + (node.height || 0) / 2;

    // Smooth zoom and pan to element
    reactFlowInstance.setCenter(x, y, {
        zoom: opts.zoom,
        duration: opts.duration,
    });

    // Add highlight effect
    if (opts.highlight) {
        highlightElement(tableId, opts.highlightDuration!);
    }
}

/**
 * Focus on a relationship edge
 */
export function focusOnRelationship(
    relationshipId: string,
    edges: Edge[],
    nodes: Node[],
    reactFlowInstance: any,
    options: FocusOptions = {}
) {
    const opts = { ...DEFAULT_OPTIONS, ...options };
    const edge = edges.find((e) => e.id === relationshipId);

    if (!edge || !reactFlowInstance) {
        console.warn('Cannot focus: edge or reactFlowInstance not found');
        return;
    }

    // Find source and target nodes
    const sourceNode = nodes.find((n) => n.id === edge.source);
    const targetNode = nodes.find((n) => n.id === edge.target);

    if (!sourceNode || !targetNode) {
        console.warn('Cannot focus: source or target node not found');
        return;
    }

    // Calculate midpoint between tables
    const sourceX = sourceNode.position.x + (sourceNode.width || 0) / 2;
    const sourceY = sourceNode.position.y + (sourceNode.height || 0) / 2;
    const targetX = targetNode.position.x + (targetNode.width || 0) / 2;
    const targetY = targetNode.position.y + (targetNode.height || 0) / 2;

    const midX = (sourceX + targetX) / 2;
    const midY = (sourceY + targetY) / 2;

    // Zoom to show both tables and the relationship
    reactFlowInstance.setCenter(midX, midY, {
        zoom: opts.zoom! * 0.8, // Slightly less zoom to show both tables
        duration: opts.duration,
    });

    // Highlight both tables and the edge
    if (opts.highlight) {
        highlightElement(edge.source, opts.highlightDuration!);
        highlightElement(edge.target, opts.highlightDuration!);
        highlightEdge(relationshipId, opts.highlightDuration!);
    }
}

/**
 * Add temporary highlight class to an element
 */
function highlightElement(elementId: string, duration: number) {
    // Find element in DOM
    const element = document.querySelector(
        `[data-id="${elementId}"], [data-nodeid="${elementId}"]`
    );

    if (!element) {
        console.warn('Cannot highlight: element not found in DOM');
        return;
    }

    // Add highlight class
    element.classList.add('ai-highlight');

    // Add pulsing animation
    const style = document.createElement('style');
    style.textContent = `
        .ai-highlight {
            animation: ai-pulse 0.6s ease-in-out 3;
            outline: 3px solid #3b82f6 !important;
            outline-offset: 4px;
            border-radius: 8px;
            box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2) !important;
        }
        
        @keyframes ai-pulse {
            0%, 100% {
                box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
            }
            50% {
                box-shadow: 0 0 0 8px rgba(59, 130, 246, 0.4);
            }
        }
    `;
    document.head.appendChild(style);

    // Remove after duration
    setTimeout(() => {
        element.classList.remove('ai-highlight');
        style.remove();
    }, duration);
}

/**
 * Highlight a relationship edge
 */
function highlightEdge(edgeId: string, duration: number) {
    // Find edge path element
    const edgePath = document.querySelector(
        `[data-edgeid="${edgeId}"] path, [data-id="${edgeId}"] path`
    );

    if (!edgePath) {
        console.warn('Cannot highlight edge: path not found in DOM');
        return;
    }

    // Store original stroke
    const originalStroke = edgePath.getAttribute('stroke');
    const originalStrokeWidth = edgePath.getAttribute('stroke-width');

    // Highlight
    edgePath.setAttribute('stroke', '#3b82f6');
    edgePath.setAttribute('stroke-width', '3');
    edgePath.classList.add('ai-edge-highlight');

    // Add animation style
    const style = document.createElement('style');
    style.textContent = `
        .ai-edge-highlight {
            animation: ai-edge-pulse 0.6s ease-in-out 3;
            filter: drop-shadow(0 0 8px rgba(59, 130, 246, 0.6));
        }
        
        @keyframes ai-edge-pulse {
            0%, 100% {
                opacity: 1;
            }
            50% {
                opacity: 0.6;
            }
        }
    `;
    document.head.appendChild(style);

    // Restore after duration
    setTimeout(() => {
        if (originalStroke) edgePath.setAttribute('stroke', originalStroke);
        if (originalStrokeWidth)
            edgePath.setAttribute('stroke-width', originalStrokeWidth);
        edgePath.classList.remove('ai-edge-highlight');
        style.remove();
    }, duration);
}

/**
 * Auto-fit the diagram to show all elements
 */
export function fitDiagram(reactFlowInstance: any, padding: number = 50) {
    if (!reactFlowInstance) {
        console.warn('Cannot fit diagram: reactFlowInstance not found');
        return;
    }

    reactFlowInstance.fitView({
        padding,
        duration: 500,
    });
}
