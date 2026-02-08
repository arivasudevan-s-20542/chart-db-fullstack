# ChartDB MCP Server Integration

## Overview

ChartDB now exposes its full functionality through the **Model Context Protocol (MCP)**, allowing AI agents like Claude Desktop, Continue, and other MCP-compatible clients to interact programmatically with diagram management, schema design, and database operations.

## What is MCP?

Model Context Protocol is a standard protocol that enables AI applications to interact with external tools and data sources. ChartDB implements MCP to expose its database diagram management capabilities as tools that AI agents can call.

## Features

### 🛠️ Available Tools (25+)

#### Diagram Management
- `chartdb/get-diagram` - Get diagram by ID
- `chartdb/get-diagram-full` - Get full diagram with all tables, columns, and relationships
- `chartdb/create-diagram` - Create a new diagram
- `chartdb/update-diagram` - Update diagram properties
- `chartdb/delete-diagram` - Delete a diagram
- `chartdb/list-diagrams` - List user's diagrams

#### Table Operations
- `chartdb/create-table` - Create a new table in diagram
- `chartdb/update-table` - Update table properties
- `chartdb/delete-table` - Delete a table
- `chartdb/move-table` - Move table position on canvas
- `chartdb/list-tables` - List all tables in diagram

#### Column Operations
- `chartdb/create-column` - Add column to table
- `chartdb/update-column` - Update column properties
- `chartdb/delete-column` - Delete a column
- `chartdb/reorder-columns` - Reorder columns in table

#### Relationship Operations
- `chartdb/create-relationship` - Create relationship between tables
- `chartdb/update-relationship` - Update relationship
- `chartdb/delete-relationship` - Delete a relationship
- `chartdb/list-relationships` - List all relationships in diagram

#### Export & Schema
- `chartdb/export-sql` - Export diagram as SQL DDL (PostgreSQL, MySQL, SQLite, etc.)
- `chartdb/export-json` - Export diagram as JSON

#### Database Connections
- `chartdb/create-connection` - Create database connection
- `chartdb/test-connection` - Test database connection
- `chartdb/list-connections` - List database connections for diagram

#### Query Execution
- `chartdb/execute-query` - Execute SQL query
- `chartdb/get-query-history` - Get query execution history

### 📦 Resources

Resources provide read-only access to diagram data:

- `chartdb://diagram/{id}` - Diagram resource
- `chartdb://diagram/{id}/schema` - Full diagram schema
- `chartdb://diagram/{id}/tables` - Diagram tables
- `chartdb://diagram/{id}/relationships` - Diagram relationships

### 💡 Prompts

Pre-configured prompts for common AI tasks:

- `analyze-schema` - Analyze diagram schema and suggest improvements
- `generate-migration` - Generate migration script from schema changes
- `suggest-indexes` - Suggest database indexes based on relationships
- `normalize-schema` - Suggest schema normalization improvements

## Setup

### 1. Enable MCP Server

The MCP server is automatically enabled when you start ChartDB. The discovery endpoint is available at:

```
GET http://localhost:8080/api/mcp/.well-known/mcp.json
```

### 2. Configure Claude Desktop

Add ChartDB MCP server to your Claude Desktop configuration:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "chartdb": {
      "command": "node",
      "args": ["-e", "require('http').get('http://localhost:8080/api/mcp/.well-known/mcp.json', (res) => { let data = ''; res.on('data', chunk => data += chunk); res.on('end', () => console.log(data)); })"]
    }
  }
}
```

Or use the simplified configuration with authentication:

```json
{
  "mcpServers": {
    "chartdb": {
      "url": "http://localhost:8080/api/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_JWT_TOKEN"
      }
    }
  }
}
```

### 3. Get JWT Token

To authenticate with ChartDB MCP server, you need a JWT token:

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "yourpassword"
  }'

# Response:
# {
#   "success": true,
#   "data": {
#     "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
#     "tokenType": "Bearer"
#   }
# }
```

Use the `accessToken` in your MCP configuration.

### 4. Configure Continue VS Code Extension

Add to `.continue/config.json`:

```json
{
  "models": [...],
  "mcpServers": [
    {
      "name": "chartdb",
      "url": "http://localhost:8080/api/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_JWT_TOKEN"
      }
    }
  ]
}
```

## Usage Examples

### Example 1: Create a Diagram with AI

**Prompt to Claude:**
> "Using ChartDB MCP, create a new e-commerce diagram with products and orders tables"

**AI will execute:**
```
1. chartdb/create-diagram(name: "E-commerce Schema", databaseType: "postgresql")
2. chartdb/create-table(diagramId: "...", name: "products", columns: [...])
3. chartdb/create-table(diagramId: "...", name: "orders", columns: [...])
4. chartdb/create-relationship(diagramId: "...", sourceTableId: "...", targetTableId: "...")
```

### Example 2: Analyze Existing Diagram

**Prompt to Claude:**
> "Analyze my diagram ABC123 and suggest improvements"

**AI will execute:**
```
1. chartdb/get-diagram-full(diagramId: "ABC123")
2. Analyze schema structure
3. Use 'analyze-schema' prompt to generate suggestions
```

### Example 3: Generate Migration Script

**Prompt to Claude:**
> "Export diagram XYZ789 as PostgreSQL SQL"

**AI will execute:**
```
chartdb/export-sql(diagramId: "XYZ789", dialect: "postgresql")
```

### Example 4: Database Query Execution

**Prompt to Claude:**
> "Connect to my production database and show me all users"

**AI will execute:**
```
1. chartdb/test-connection(host: "...", port: 5432, ...)
2. chartdb/create-connection(diagramId: "...", name: "Production", ...)
3. chartdb/execute-query(connectionId: "...", query: "SELECT * FROM users LIMIT 10")
```

## API Reference

### Tool Call Request Format

```bash
POST /api/mcp/tools/call
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "chartdb/create-table",
  "arguments": {
    "diagramId": "abc-123",
    "name": "users",
    "schema": "public",
    "columns": [
      {
        "name": "id",
        "type": "INTEGER",
        "primaryKey": true,
        "nullable": false
      },
      {
        "name": "email",
        "type": "VARCHAR(255)",
        "unique": true,
        "nullable": false
      }
    ]
  }
}
```

### Tool Call Response Format

```json
{
  "success": true,
  "message": "Tool executed successfully",
  "data": {
    "id": "table-456",
    "name": "users",
    "diagramId": "abc-123",
    "columns": [...],
    "createdAt": "2026-02-08T10:00:00Z"
  }
}
```

### Resource Access

```bash
GET /api/mcp/resources/diagram/abc-123/schema
Authorization: Bearer YOUR_JWT_TOKEN

# Response: Full diagram schema with tables, columns, relationships
```

## Security

### Authentication

All MCP endpoints (except discovery) require JWT authentication:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

### Rate Limiting

MCP tool calls respect the same rate limits as regular API calls:
- 100 requests per minute per user
- 1000 requests per hour per user

### Permissions

MCP tools inherit the same permission system as the web interface:
- Users can only access their own diagrams
- Shared diagram permissions are respected
- Database connections are encrypted

## Error Handling

### Common Errors

```json
{
  "success": false,
  "message": "Unknown tool: chartdb/invalid-tool",
  "error": "TOOL_NOT_FOUND"
}
```

```json
{
  "success": false,
  "message": "Diagram not found",
  "error": "RESOURCE_NOT_FOUND"
}
```

```json
{
  "success": false,
  "message": "Unauthorized access to diagram",
  "error": "FORBIDDEN"
}
```

## Best Practices

### 1. Always Get Full Context First

Before modifying a diagram, fetch full context:

```
chartdb/get-diagram-full(diagramId: "...")
```

### 2. Use Batch Operations

Instead of creating tables one by one, create them in a single conversation turn.

### 3. Validate Before Apply

Use AI to analyze changes before applying them:

```
1. Get current schema
2. Generate proposed changes
3. Show diff to user
4. Apply changes only if approved
```

### 4. Export After Changes

Always export schema after major changes:

```
chartdb/export-sql(diagramId: "...", dialect: "postgresql")
```

### 5. Test Database Connections

Before executing queries, test connections:

```
chartdb/test-connection(host: "...", port: 5432, ...)
```

## Troubleshooting

### MCP Server Not Discovered

1. Verify ChartDB is running: `curl http://localhost:8080/api/v1/health`
2. Check MCP endpoint: `curl http://localhost:8080/api/mcp/.well-known/mcp.json`
3. Restart Claude Desktop / VS Code

### Authentication Errors

1. Verify JWT token is valid: `curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/diagrams`
2. Token may have expired - login again to get new token
3. Check token format: Should be `Bearer eyJhbGc...`

### Tool Not Found

1. Check tool name spelling: `chartdb/create-table` (not `create-table`)
2. Verify MCP server version: `GET /api/mcp/.well-known/mcp.json`
3. Update Claude Desktop / Continue extension

## Integration Examples

### Python Client

```python
import requests

class ChartDBMCP:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {"Authorization": f"Bearer {token}"}
    
    def call_tool(self, tool_name, **arguments):
        response = requests.post(
            f"{self.base_url}/api/mcp/tools/call",
            headers=self.headers,
            json={"name": tool_name, "arguments": arguments}
        )
        return response.json()

# Usage
client = ChartDBMCP("http://localhost:8080", "your-jwt-token")
result = client.call_tool("chartdb/create-diagram", name="My Diagram")
print(result)
```

### Node.js Client

```javascript
class ChartDBMCP {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async callTool(toolName, arguments) {
    const response = await fetch(`${this.baseUrl}/api/mcp/tools/call`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: toolName, arguments })
    });
    return response.json();
  }
}

// Usage
const client = new ChartDBMCP('http://localhost:8080', 'your-jwt-token');
const result = await client.callTool('chartdb/create-diagram', { name: 'My Diagram' });
console.log(result);
```

## Roadmap

### Coming Soon

- [ ] Streaming responses for long-running operations
- [ ] Webhook support for diagram changes
- [ ] Batch tool execution
- [ ] Schema validation and linting tools
- [ ] AI-powered schema suggestions
- [ ] Migration generation from diff
- [ ] Database reverse engineering
- [ ] Performance analysis tools

## Support

- **Documentation**: [https://chartdb.io/docs/mcp](https://chartdb.io/docs/mcp)
- **Issues**: [https://github.com/chartdb/chartdb/issues](https://github.com/chartdb/chartdb/issues)
- **Discord**: [https://discord.gg/chartdb](https://discord.gg/chartdb)

## License

ChartDB MCP Server is part of ChartDB and follows the same license.

---

**Version**: 1.20.2  
**Last Updated**: February 8, 2026
