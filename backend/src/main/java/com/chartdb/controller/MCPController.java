package com.chartdb.controller;

import com.chartdb.dto.mcp.*;
import com.chartdb.dto.request.*;
import com.chartdb.dto.response.*;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP (Model Context Protocol) Controller
 * Exposes ChartDB functionalities as MCP tools for AI agents
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class MCPController {
    
    private final DiagramService diagramService;
    private final TableService tableService;
    private final ColumnService columnService;
    private final RelationshipService relationshipService;
    private final ExportService exportService;
    private final DatabaseConnectionService connectionService;
    private final QueryExecutionService queryExecutionService;
    private final ObjectMapper objectMapper;
    
    // ==========================================
    // MCP Streamable HTTP / JSON-RPC endpoint
    // ==========================================

    /**
     * GET handler for MCP endpoint. Streamable HTTP clients may probe this
     * for SSE streams. We don't support SSE, so return 405 Method Not Allowed.
     * This prevents Spring from returning 401 for unhandled methods.
     */
    @GetMapping(value = {"", "/"})
    public ResponseEntity<Map<String, String>> handleGet() {
        return ResponseEntity.ok(Map.of(
            "name", "ChartDB MCP Server",
            "version", "1.20.2",
            "protocol", "MCP JSON-RPC 2.0 over Streamable HTTP",
            "endpoint", "POST /api/mcp"
        ));
    }
    
    /**
     * MCP JSON-RPC endpoint for Streamable HTTP transport.
     * Handles VS Code and Claude Desktop MCP protocol messages.
     */
    @SuppressWarnings("unchecked")
    @PostMapping(value = "", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> handleJsonRpc(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> request) {
        
        Object id = request.get("id");
        String method = (String) request.get("method");
        Map<String, Object> params = request.containsKey("params") 
            ? (Map<String, Object>) request.get("params") 
            : Map.of();
        
        log.info("MCP JSON-RPC: method={}, id={}, authenticated={}", method, id, currentUser != null);
        
        // Notifications (no id) - return 202 Accepted with no body
        if (id == null) {
            return ResponseEntity.accepted().build();
        }
        
        try {
            // Methods that don't require authentication
            Object result = switch (method) {
                case "initialize" -> handleInitialize();
                case "ping" -> Map.of();
                case "tools/list" -> handleToolsList();
                case "prompts/list" -> handlePromptsList();
                case "resources/list" -> handleResourcesList();
                // Methods that require authentication
                case "tools/call" -> {
                    requireAuth(currentUser);
                    yield handleToolsCallJsonRpc(currentUser, params);
                }
                case "resources/read" -> {
                    requireAuth(currentUser);
                    yield handleResourcesRead(currentUser, params);
                }
                case "prompts/get" -> handlePromptsGet(params);
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };
            
            return ResponseEntity.ok(jsonRpcResponse(id, result));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(jsonRpcError(id, -32601, e.getMessage()));
        } catch (SecurityException e) {
            // IMPORTANT: Return HTTP 200 with JSON-RPC error, NOT HTTP 401.
            // HTTP 401 triggers VS Code's OAuth discovery flow which we don't support.
            log.warn("MCP auth required but no valid token provided for method={}", method);
            return ResponseEntity.ok(jsonRpcError(id, -32600, "Authentication required. Provide a valid MCP API token as Bearer token in the Authorization header."));
        } catch (Exception e) {
            log.error("MCP JSON-RPC error: method={}", method, e);
            return ResponseEntity.ok(jsonRpcError(id, -32603, e.getMessage()));
        }
    }
    
    private void requireAuth(UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new SecurityException("Authentication required for this operation");
        }
    }
    
    private Map<String, Object> handleInitialize() {
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of("listChanged", false),
                "resources", Map.of("listChanged", false),
                "prompts", Map.of("listChanged", false)
            ),
            "serverInfo", Map.of(
                "name", "ChartDB MCP Server",
                "version", "1.20.2"
            )
        );
    }
    
    private Map<String, Object> handleToolsList() {
        List<Map<String, Object>> tools = getAvailableTools().stream()
            .map(this::toolToJsonSchema)
            .collect(Collectors.toList());
        return Map.of("tools", tools);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCallJsonRpc(UserPrincipal currentUser, Map<String, Object> params) {
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = params.containsKey("arguments") 
            ? (Map<String, Object>) params.get("arguments") 
            : Map.of();
        
        MCPToolCall toolCall = MCPToolCall.builder()
            .name(toolName)
            .arguments(arguments)
            .build();
        
        Object result = executeToolCall(currentUser, toolCall);
        
        // Wrap in MCP content format
        String text;
        try {
            text = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            text = String.valueOf(result);
        }
        
        return Map.of(
            "content", List.of(Map.of("type", "text", "text", text)),
            "isError", false
        );
    }
    
    private Map<String, Object> handleResourcesList() {
        List<Map<String, Object>> resources = getAvailableResources().stream()
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uri", r.getUri());
                m.put("name", r.getDescription());
                m.put("mimeType", r.getMimeType());
                return m;
            })
            .collect(Collectors.toList());
        return Map.of("resources", resources);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleResourcesRead(UserPrincipal currentUser, Map<String, Object> params) {
        String uri = (String) params.get("uri");
        // Parse URI like "chartdb://diagram/123/schema"
        String path = uri.replace("chartdb://", "");
        String[] parts = path.split("/");
        
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid resource URI: " + uri);
        }
        
        String resourceType = parts[0];
        String diagramId = parts[1];
        String subResource = parts.length > 2 ? parts[2] : null;
        
        Object data = switch (resourceType) {
            case "diagram" -> {
                if ("schema".equals(subResource)) {
                    yield diagramService.getFullDiagram(diagramId, currentUser.getId());
                } else if ("tables".equals(subResource)) {
                    yield tableService.getDiagramTables(diagramId, currentUser.getId());
                } else if ("relationships".equals(subResource)) {
                    yield relationshipService.getDiagramRelationships(diagramId, currentUser.getId());
                } else {
                    yield diagramService.getDiagram(diagramId, currentUser.getId());
                }
            }
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        };
        
        String text;
        try {
            text = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            text = String.valueOf(data);
        }
        
        return Map.of("contents", List.of(Map.of(
            "uri", uri,
            "mimeType", "application/json",
            "text", text
        )));
    }
    
    private Map<String, Object> handlePromptsList() {
        List<Map<String, Object>> prompts = getAvailablePrompts().stream()
            .map(p -> {
                List<Map<String, Object>> args = p.getParameters().entrySet().stream()
                    .map(e -> {
                        Map<String, Object> arg = new LinkedHashMap<>();
                        arg.put("name", e.getKey());
                        arg.put("description", e.getValue().getDescription());
                        arg.put("required", e.getValue().isRequired());
                        return arg;
                    })
                    .collect(Collectors.toList());
                return Map.<String, Object>of(
                    "name", p.getName(),
                    "description", p.getDescription(),
                    "arguments", args
                );
            })
            .collect(Collectors.toList());
        return Map.of("prompts", prompts);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePromptsGet(Map<String, Object> params) {
        String name = (String) params.get("name");
        return Map.of(
            "description", "Prompt: " + name,
            "messages", List.of(Map.of(
                "role", "user",
                "content", Map.of("type", "text", "text", "Execute prompt: " + name)
            ))
        );
    }
    
    // Convert MCPTool to JSON Schema format for tools/list
    private Map<String, Object> toolToJsonSchema(MCPTool tool) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        
        if (tool.getParameters() != null) {
            tool.getParameters().forEach((paramName, param) -> {
                Map<String, Object> prop = new LinkedHashMap<>();
                prop.put("type", param.getType());
                prop.put("description", param.getDescription());
                // JSON Schema requires "items" for array types
                if ("array".equals(param.getType())) {
                    if (param.getItems() != null) {
                        prop.put("items", param.getItems());
                    } else {
                        // Default: array of strings
                        prop.put("items", Map.of("type", "string"));
                    }
                }
                properties.put(paramName, prop);
                if (param.isRequired()) {
                    required.add(paramName);
                }
            });
        }
        
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", tool.getName());
        result.put("description", tool.getDescription());
        result.put("inputSchema", inputSchema);
        return result;
    }
    
    private Map<String, Object> jsonRpcResponse(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }
    
    private Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message != null ? message : "Internal error"));
        return response;
    }
    
    /**
     * OAuth Protected Resource Metadata (RFC 9728).
     * Returns metadata indicating Bearer tokens are accepted but NO authorization server exists.
     * This prevents MCP clients from triggering OAuth flows — they should use static API tokens.
     */
    @GetMapping(value = "/.well-known/oauth-protected-resource", produces = "application/json")
    public ResponseEntity<Map<String, Object>> mcpOAuthProtectedResource(
            jakarta.servlet.http.HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return ResponseEntity.ok(Map.of(
            "resource", baseUrl + "/api/mcp",
            "bearer_methods_supported", List.of("header"),
            "resource_documentation", "https://chartdb.in/docs/mcp"
        ));
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public ResponseEntity<Void> mcpOAuthAuthorizationServer() {
        return ResponseEntity.notFound().build();
    }

    /**
     * MCP Discovery endpoint - Returns available tools, resources, and prompts
     */
    @GetMapping("/.well-known/mcp.json")
    public ResponseEntity<MCPServerManifest> getManifest() {
        MCPServerManifest manifest = MCPServerManifest.builder()
            .name("ChartDB MCP Server")
            .version("1.20.2")
            .description("Model Context Protocol server for ChartDB - Database diagram and schema management")
            .capabilities(MCPCapabilities.builder()
                .tools(true)
                .resources(true)
                .prompts(true)
                .build())
            .tools(getAvailableTools())
            .resources(getAvailableResources())
            .prompts(getAvailablePrompts())
            .build();
        
        return ResponseEntity.ok(manifest);
    }
    
    /**
     * Execute MCP tool call (REST endpoint)
     */
    @PostMapping("/tools/call")
    public ResponseEntity<ApiResponse<Object>> callTool(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody MCPToolCall toolCall) {
        
        Object result = executeToolCall(currentUser, toolCall);
        return ResponseEntity.ok(ApiResponse.success("Tool executed successfully", result));
    }
    
    /**
     * Shared tool execution logic used by both REST and JSON-RPC endpoints
     */
    private Object executeToolCall(UserPrincipal currentUser, MCPToolCall toolCall) {
        return switch (toolCall.getName()) {
            // Diagram tools
            case "chartdb_get-diagram" -> getDiagram(currentUser, toolCall);
            case "chartdb_get-diagram-full" -> getDiagramFull(currentUser, toolCall);
            case "chartdb_create-diagram" -> createDiagram(currentUser, toolCall);
            case "chartdb_update-diagram" -> updateDiagram(currentUser, toolCall);
            case "chartdb_delete-diagram" -> deleteDiagram(currentUser, toolCall);
            case "chartdb_list-diagrams" -> listDiagrams(currentUser, toolCall);
            
            // Table tools
            case "chartdb_create-table" -> createTable(currentUser, toolCall);
            case "chartdb_update-table" -> updateTable(currentUser, toolCall);
            case "chartdb_delete-table" -> deleteTable(currentUser, toolCall);
            case "chartdb_move-table" -> moveTable(currentUser, toolCall);
            case "chartdb_list-tables" -> listTables(currentUser, toolCall);
            
            // Column tools
            case "chartdb_create-column" -> createColumn(currentUser, toolCall);
            case "chartdb_update-column" -> updateColumn(currentUser, toolCall);
            case "chartdb_delete-column" -> deleteColumn(currentUser, toolCall);
            case "chartdb_reorder-columns" -> reorderColumns(currentUser, toolCall);
            
            // Relationship tools
            case "chartdb_create-relationship" -> createRelationship(currentUser, toolCall);
            case "chartdb_update-relationship" -> updateRelationship(currentUser, toolCall);
            case "chartdb_delete-relationship" -> deleteRelationship(currentUser, toolCall);
            case "chartdb_list-relationships" -> listRelationships(currentUser, toolCall);
            
            // Export tools
            case "chartdb_export-sql" -> exportSql(currentUser, toolCall);
            case "chartdb_export-json" -> exportJson(currentUser, toolCall);
            
            // Database connection tools
            case "chartdb_create-connection" -> createConnection(currentUser, toolCall);
            case "chartdb_test-connection" -> testConnection(currentUser, toolCall);
            case "chartdb_list-connections" -> listConnections(currentUser, toolCall);
            
            // Query tools
            case "chartdb_execute-query" -> executeQuery(currentUser, toolCall);
            case "chartdb_get-query-history" -> getQueryHistory(currentUser, toolCall);
            
            default -> throw new IllegalArgumentException("Unknown tool: " + toolCall.getName());
        };
    }
    
    /**
     * Get MCP resource
     */
    @GetMapping("/resources/{resourceUri}")
    public ResponseEntity<ApiResponse<Object>> getResource(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String resourceUri) {
        
        // Parse resource URI (e.g., "chartdb://diagram/123/schema")
        String[] parts = resourceUri.split("/");
        
        if (parts.length < 3) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid resource URI"));
        }
        
        String resourceType = parts[0];
        String diagramId = parts[1];
        String subResource = parts.length > 2 ? parts[2] : null;
        
        Object result = switch (resourceType) {
            case "diagram" -> {
                if ("schema".equals(subResource)) {
                    yield diagramService.getFullDiagram(diagramId, currentUser.getId());
                } else if ("tables".equals(subResource)) {
                    yield tableService.getDiagramTables(diagramId, currentUser.getId());
                } else if ("relationships".equals(subResource)) {
                    yield relationshipService.getDiagramRelationships(diagramId, currentUser.getId());
                } else {
                    yield diagramService.getDiagram(diagramId, currentUser.getId());
                }
            }
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        };
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    // Diagram operations
    private DiagramResponse getDiagram(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return diagramService.getDiagram(diagramId, user.getId());
    }
    
    private DiagramFullResponse getDiagramFull(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return diagramService.getFullDiagram(diagramId, user.getId());
    }
    
    private DiagramResponse createDiagram(UserPrincipal user, MCPToolCall call) {
        CreateDiagramRequest request = mapToRequest(call.getArguments(), CreateDiagramRequest.class);
        return diagramService.createDiagram(user.getId(), request);
    }
    
    private DiagramResponse updateDiagram(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        UpdateDiagramRequest request = mapToRequest(call.getArguments(), UpdateDiagramRequest.class);
        return diagramService.updateDiagram(diagramId, user.getId(), request);
    }
    
    private Map<String, Object> deleteDiagram(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        diagramService.deleteDiagram(diagramId, user.getId());
        return Map.of("success", true, "message", "Diagram deleted");
    }
    
    private List<DiagramSummaryResponse> listDiagrams(UserPrincipal user, MCPToolCall call) {
        int limit = call.getArguments().containsKey("limit") 
            ? (Integer) call.getArguments().get("limit") 
            : 10;
        return diagramService.getRecentDiagrams(user.getId(), limit);
    }
    
    // Table operations
    private TableResponse createTable(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        CreateTableRequest request = mapToRequest(call.getArguments(), CreateTableRequest.class);
        return tableService.createTable(diagramId, user.getId(), request);
    }
    
    private TableResponse updateTable(UserPrincipal user, MCPToolCall call) {
        String tableId = call.getArguments().get("tableId").toString();
        UpdateTableRequest request = mapToRequest(call.getArguments(), UpdateTableRequest.class);
        return tableService.updateTable(tableId, user.getId(), request);
    }
    
    private Map<String, Object> deleteTable(UserPrincipal user, MCPToolCall call) {
        String tableId = call.getArguments().get("tableId").toString();
        tableService.deleteTable(tableId, user.getId());
        return Map.of("success", true, "message", "Table deleted");
    }
    
    private TableResponse moveTable(UserPrincipal user, MCPToolCall call) {
        String tableId = call.getArguments().get("tableId").toString();
        MoveTableRequest request = MoveTableRequest.builder()
            .positionX(new java.math.BigDecimal(call.getArguments().get("x").toString()))
            .positionY(new java.math.BigDecimal(call.getArguments().get("y").toString()))
            .build();
        return tableService.moveTable(tableId, user.getId(), request);
    }
    
    private List<TableResponse> listTables(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return tableService.getDiagramTables(diagramId, user.getId());
    }
    
    // Column operations
    private ColumnResponse createColumn(UserPrincipal user, MCPToolCall call) {
        String tableId = call.getArguments().get("tableId").toString();
        CreateColumnRequest request = mapToRequest(call.getArguments(), CreateColumnRequest.class);
        return columnService.createColumn(tableId, user.getId(), request);
    }
    
    private ColumnResponse updateColumn(UserPrincipal user, MCPToolCall call) {
        String columnId = call.getArguments().get("columnId").toString();
        UpdateColumnRequest request = mapToRequest(call.getArguments(), UpdateColumnRequest.class);
        return columnService.updateColumn(columnId, user.getId(), request);
    }
    
    private Map<String, Object> deleteColumn(UserPrincipal user, MCPToolCall call) {
        String columnId = call.getArguments().get("columnId").toString();
        columnService.deleteColumn(columnId, user.getId());
        return Map.of("success", true, "message", "Column deleted");
    }
    
    private Map<String, Object> reorderColumns(UserPrincipal user, MCPToolCall call) {
        String tableId = call.getArguments().get("tableId").toString();
        @SuppressWarnings("unchecked")
        List<String> columnIds = (List<String>) call.getArguments().get("columnIds");
        columnService.reorderColumns(tableId, user.getId(), columnIds);
        return Map.of("success", true, "message", "Columns reordered");
    }
    
    // Relationship operations
    private RelationshipResponse createRelationship(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        CreateRelationshipRequest request = mapToRequest(call.getArguments(), CreateRelationshipRequest.class);
        return relationshipService.createRelationship(diagramId, user.getId(), request);
    }
    
    private RelationshipResponse updateRelationship(UserPrincipal user, MCPToolCall call) {
        String relationshipId = call.getArguments().get("relationshipId").toString();
        UpdateRelationshipRequest request = mapToRequest(call.getArguments(), UpdateRelationshipRequest.class);
        return relationshipService.updateRelationship(relationshipId, user.getId(), request);
    }
    
    private Map<String, Object> deleteRelationship(UserPrincipal user, MCPToolCall call) {
        String relationshipId = call.getArguments().get("relationshipId").toString();
        relationshipService.deleteRelationship(relationshipId, user.getId());
        return Map.of("success", true, "message", "Relationship deleted");
    }
    
    private List<RelationshipResponse> listRelationships(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return relationshipService.getDiagramRelationships(diagramId, user.getId());
    }
    
    // Export operations
    private String exportSql(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        String dialect = call.getArguments().getOrDefault("dialect", "postgresql").toString();
        return exportService.exportAsSql(diagramId, dialect, user.getId());
    }
    
    private String exportJson(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return exportService.exportAsJson(diagramId, user.getId());
    }
    
    // Database connection operations
    private ConnectionResponse createConnection(UserPrincipal user, MCPToolCall call) {
        Map<String, Object> args = call.getArguments();
        CreateConnectionRequest request = CreateConnectionRequest.builder()
            .diagramId(args.get("diagramId").toString())
            .name(args.get("name").toString())
            .databaseType(args.get("type").toString())
            .host(args.get("host").toString())
            .port(Integer.valueOf(args.get("port").toString()))
            .databaseName(args.get("database").toString())
            .username(args.get("username").toString())
            .password(args.get("password").toString())
            .build();
        return connectionService.createConnection(user.getId(), request);
    }
    
    private ConnectionTestResult testConnection(UserPrincipal user, MCPToolCall call) {
        Map<String, Object> args = call.getArguments();
        TestConnectionRequest request = TestConnectionRequest.builder()
            .databaseType(args.containsKey("type") ? args.get("type").toString() : "postgresql")
            .host(args.get("host").toString())
            .port(Integer.valueOf(args.get("port").toString()))
            .databaseName(args.get("database").toString())
            .username(args.get("username").toString())
            .password(args.get("password").toString())
            .build();
        return connectionService.testConnection(request);
    }
    
    private List<ConnectionResponse> listConnections(UserPrincipal user, MCPToolCall call) {
        String diagramId = call.getArguments().get("diagramId").toString();
        return connectionService.getConnectionsByDiagram(diagramId, user.getId());
    }
    
    // Query operations
    private QueryExecutionResult executeQuery(UserPrincipal user, MCPToolCall call) {
        String connectionId = call.getArguments().get("connectionId").toString();
        ExecuteQueryRequest request = mapToRequest(call.getArguments(), ExecuteQueryRequest.class);
        return queryExecutionService.executeQuery(connectionId, user.getId(), request);
    }
    
    private List<QueryHistoryResponse> getQueryHistory(UserPrincipal user, MCPToolCall call) {
        // For simplicity, return last 20 queries
        return queryExecutionService.getQueryHistory(user.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
            .getContent();
    }
    
    // Helper methods
    private List<MCPTool> getAvailableTools() {
        return List.of(
            // Diagram tools
            MCPTool.of("chartdb_get-diagram", "Get diagram by ID", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            MCPTool.of("chartdb_get-diagram-full", "Get full diagram with all tables, columns, and relationships", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            MCPTool.of("chartdb_create-diagram", "Create a new diagram", 
                Map.of("name", MCPParameter.required("string", "Diagram name"),
                       "databaseType", MCPParameter.optional("string", "Database type (postgresql, mysql, etc.)"))),
            MCPTool.of("chartdb_update-diagram", "Update diagram properties", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "name", MCPParameter.optional("string", "New name"))),
            MCPTool.of("chartdb_delete-diagram", "Delete a diagram", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            MCPTool.of("chartdb_list-diagrams", "List user's diagrams", 
                Map.of("limit", MCPParameter.optional("number", "Maximum number of diagrams to return"))),
            
            // Table tools
            MCPTool.of("chartdb_create-table", "Create a new table in diagram", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "name", MCPParameter.required("string", "Table name"),
                       "schema", MCPParameter.optional("string", "Schema name"),
                       "columns", MCPParameter.optionalArray("Array of column definitions", Map.of(
                           "type", "object",
                           "properties", Map.of(
                               "name", Map.of("type", "string", "description", "Column name"),
                               "type", Map.of("type", "string", "description", "Data type (e.g. varchar, integer, boolean)"),
                               "nullable", Map.of("type", "boolean", "description", "Is nullable"),
                               "primaryKey", Map.of("type", "boolean", "description", "Is primary key"),
                               "unique", Map.of("type", "boolean", "description", "Is unique")
                           ),
                           "required", List.of("name", "type")
                       )))),
            MCPTool.of("chartdb_update-table", "Update table properties", 
                Map.of("tableId", MCPParameter.required("string", "Table ID"),
                       "name", MCPParameter.optional("string", "New table name"),
                       "color", MCPParameter.optional("string", "Table color"))),
            MCPTool.of("chartdb_delete-table", "Delete a table", 
                Map.of("tableId", MCPParameter.required("string", "Table ID"))),
            MCPTool.of("chartdb_move-table", "Move table position", 
                Map.of("tableId", MCPParameter.required("string", "Table ID"),
                       "x", MCPParameter.required("number", "X coordinate"),
                       "y", MCPParameter.required("number", "Y coordinate"))),
            MCPTool.of("chartdb_list-tables", "List all tables in diagram", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            
            // Column tools
            MCPTool.of("chartdb_create-column", "Add column to table", 
                Map.of("tableId", MCPParameter.required("string", "Table ID"),
                       "name", MCPParameter.required("string", "Column name"),
                       "type", MCPParameter.required("string", "Data type"),
                       "nullable", MCPParameter.optional("boolean", "Is nullable"),
                       "primaryKey", MCPParameter.optional("boolean", "Is primary key"),
                       "unique", MCPParameter.optional("boolean", "Is unique"))),
            MCPTool.of("chartdb_update-column", "Update column properties", 
                Map.of("columnId", MCPParameter.required("string", "Column ID"),
                       "name", MCPParameter.optional("string", "New column name"),
                       "type", MCPParameter.optional("string", "New data type"))),
            MCPTool.of("chartdb_delete-column", "Delete a column", 
                Map.of("columnId", MCPParameter.required("string", "Column ID"))),
            MCPTool.of("chartdb_reorder-columns", "Reorder columns in table", 
                Map.of("tableId", MCPParameter.required("string", "Table ID"),
                       "columnIds", MCPParameter.requiredArray("Array of column IDs in new order", Map.of("type", "string")))),
            
            // Relationship tools
            MCPTool.of("chartdb_create-relationship", "Create relationship between tables", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "sourceTableId", MCPParameter.required("string", "Source table ID"),
                       "targetTableId", MCPParameter.required("string", "Target table ID"),
                       "sourceColumnId", MCPParameter.required("string", "Source column ID (FK source - typically the PK column)"),
                       "targetColumnId", MCPParameter.required("string", "Target column ID (FK target - the foreign key column)"),
                       "type", MCPParameter.required("string", "Relationship type (one-to-one, one-to-many, many-to-many)"))),
            MCPTool.of("chartdb_update-relationship", "Update relationship", 
                Map.of("relationshipId", MCPParameter.required("string", "Relationship ID"),
                       "type", MCPParameter.optional("string", "New relationship type"))),
            MCPTool.of("chartdb_delete-relationship", "Delete a relationship", 
                Map.of("relationshipId", MCPParameter.required("string", "Relationship ID"))),
            MCPTool.of("chartdb_list-relationships", "List all relationships in diagram", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            
            // Export tools
            MCPTool.of("chartdb_export-sql", "Export diagram as SQL DDL", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "dialect", MCPParameter.optional("string", "SQL dialect (postgresql, mysql, sqlite, etc.)"))),
            MCPTool.of("chartdb_export-json", "Export diagram as JSON", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            
            // Database connection tools
            MCPTool.of("chartdb_create-connection", "Create database connection", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "name", MCPParameter.required("string", "Connection name"),
                       "type", MCPParameter.required("string", "Database type"),
                       "host", MCPParameter.required("string", "Database host"),
                       "port", MCPParameter.required("number", "Database port"),
                       "database", MCPParameter.required("string", "Database name"),
                       "username", MCPParameter.required("string", "Username"),
                       "password", MCPParameter.required("string", "Password"))),
            MCPTool.of("chartdb_test-connection", "Test database connection", 
                Map.of("host", MCPParameter.required("string", "Database host"),
                       "port", MCPParameter.required("number", "Database port"),
                       "database", MCPParameter.required("string", "Database name"),
                       "username", MCPParameter.required("string", "Username"),
                       "password", MCPParameter.required("string", "Password"))),
            MCPTool.of("chartdb_list-connections", "List database connections for diagram", 
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            
            // Query tools
            MCPTool.of("chartdb_execute-query", "Execute SQL query", 
                Map.of("connectionId", MCPParameter.required("string", "Connection ID"),
                       "query", MCPParameter.required("string", "SQL query to execute"))),
            MCPTool.of("chartdb_get-query-history", "Get query execution history", 
                Map.of())
        );
    }
    
    private List<MCPResource> getAvailableResources() {
        return List.of(
            MCPResource.of("chartdb://diagram/{id}", "Diagram resource", "application/json"),
            MCPResource.of("chartdb://diagram/{id}/schema", "Full diagram schema", "application/json"),
            MCPResource.of("chartdb://diagram/{id}/tables", "Diagram tables", "application/json"),
            MCPResource.of("chartdb://diagram/{id}/relationships", "Diagram relationships", "application/json")
        );
    }
    
    private List<MCPPrompt> getAvailablePrompts() {
        return List.of(
            MCPPrompt.of("analyze-schema", "Analyze diagram schema and suggest improvements",
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            MCPPrompt.of("generate-migration", "Generate migration script from schema changes",
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"),
                       "targetDialect", MCPParameter.optional("string", "Target SQL dialect"))),
            MCPPrompt.of("suggest-indexes", "Suggest database indexes based on relationships",
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID"))),
            MCPPrompt.of("normalize-schema", "Suggest schema normalization improvements",
                Map.of("diagramId", MCPParameter.required("string", "Diagram ID")))
        );
    }
    
    @SuppressWarnings("unchecked")
    private <T> T mapToRequest(Map<String, Object> arguments, Class<T> clazz) {
        try {
            String json = objectMapper.writeValueAsString(arguments);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map arguments to request object", e);
        }
    }
}
