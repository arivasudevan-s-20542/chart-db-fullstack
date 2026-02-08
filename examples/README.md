# ChartDB MCP Examples

This directory contains example implementations and integration guides for the ChartDB Model Context Protocol (MCP) server.

## Available Examples

### 1. Node.js Client (`mcp-client-example.js`)

A comprehensive Node.js client library for integrating with ChartDB MCP server.

**Features:**
- Full MCP tool coverage
- Promise-based API
- Type-safe operations
- Error handling

**Usage:**
```javascript
const { ChartDBMCPClient } = require('./mcp-client-example');

const client = new ChartDBMCPClient('http://localhost:8080', 'YOUR_JWT_TOKEN');

// Create a diagram
const diagram = await client.createDiagram('My Schema', 'postgresql');

// Create a table
const table = await client.createTable(diagram.id, {
  name: 'users',
  columns: [
    { name: 'id', type: 'SERIAL', primaryKey: true },
    { name: 'email', type: 'VARCHAR(255)', unique: true }
  ]
});
```

### 2. Python Client (`mcp-client-example.py`)

A Python client library with type hints and comprehensive error handling.

**Features:**
- Type-annotated methods
- Requests-based HTTP client
- Pythonic API design
- Exception handling

**Usage:**
```python
from mcp_client_example import ChartDBMCPClient

client = ChartDBMCPClient('http://localhost:8080', 'YOUR_JWT_TOKEN')

# Create a diagram
diagram = client.create_diagram('My Schema', 'postgresql')

# Create a table
table = client.create_table(
    diagram['id'],
    name='users',
    columns=[
        {'name': 'id', 'type': 'SERIAL', 'primaryKey': True},
        {'name': 'email', 'type': 'VARCHAR(255)', 'unique': True}
    ]
)
```

### 3. Integration Test Script (`../test-mcp-integration.sh`)

A bash script that demonstrates the full MCP workflow.

**Features:**
- End-to-end testing
- Authentication flow
- MCP discovery
- CRUD operations
- SQL export

**Usage:**
```bash
# Set environment variables
export API_BASE_URL="http://localhost:8080"
export EMAIL="your@email.com"
export PASSWORD="yourpassword"

# Run the test
../test-mcp-integration.sh
```

## Getting Started

### Prerequisites

1. **ChartDB Server Running**
   ```bash
   # Start the backend
   cd backend
   mvn spring-boot:run
   ```

2. **JWT Token**
   ```bash
   # Login to get token
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"your@email.com","password":"yourpassword"}'
   ```

### Quick Start

#### Node.js

```bash
# Install dependencies
npm install node-fetch

# Update token in mcp-client-example.js
# Run the example
node mcp-client-example.js
```

#### Python

```bash
# Install dependencies
pip install requests

# Update token in mcp-client-example.py
# Run the example
python mcp-client-example.py
```

#### Bash

```bash
# Make executable
chmod +x test-mcp-integration.sh

# Set credentials
export EMAIL="your@email.com"
export PASSWORD="yourpassword"

# Run
./test-mcp-integration.sh
```

## MCP Tools Reference

### Diagram Management
- `chartdb/create-diagram` - Create new diagram
- `chartdb/get-diagram` - Get diagram by ID
- `chartdb/get-diagram-full` - Get full diagram with all data
- `chartdb/update-diagram` - Update diagram properties
- `chartdb/delete-diagram` - Delete diagram
- `chartdb/list-diagrams` - List user's diagrams

### Table Operations
- `chartdb/create-table` - Create table
- `chartdb/update-table` - Update table
- `chartdb/delete-table` - Delete table
- `chartdb/move-table` - Move table position
- `chartdb/list-tables` - List diagram tables

### Column Operations
- `chartdb/create-column` - Add column
- `chartdb/update-column` - Update column
- `chartdb/delete-column` - Delete column
- `chartdb/reorder-columns` - Reorder columns

### Relationship Operations
- `chartdb/create-relationship` - Create relationship
- `chartdb/update-relationship` - Update relationship
- `chartdb/delete-relationship` - Delete relationship
- `chartdb/list-relationships` - List relationships

### Export & Schema
- `chartdb/export-sql` - Export as SQL DDL
- `chartdb/export-json` - Export as JSON

### Database Connections
- `chartdb/create-connection` - Create DB connection
- `chartdb/test-connection` - Test connection
- `chartdb/list-connections` - List connections

### Query Execution
- `chartdb/execute-query` - Execute SQL query
- `chartdb/get-query-history` - Get query history

## Advanced Examples

### Building a CLI Tool

```javascript
#!/usr/bin/env node
const { ChartDBMCPClient } = require('./mcp-client-example');
const args = process.argv.slice(2);

const client = new ChartDBMCPClient(
  process.env.CHARTDB_URL || 'http://localhost:8080',
  process.env.CHARTDB_TOKEN
);

const command = args[0];

switch (command) {
  case 'list':
    const diagrams = await client.listDiagrams();
    console.table(diagrams);
    break;
  
  case 'create':
    const diagram = await client.createDiagram(args[1]);
    console.log(`Created: ${diagram.id}`);
    break;
  
  case 'export':
    const sql = await client.exportSQL(args[1], args[2] || 'postgresql');
    console.log(sql);
    break;
}
```

### AI Agent Integration

```python
from mcp_client_example import ChartDBMCPClient
import anthropic

client = ChartDBMCPClient('http://localhost:8080', 'YOUR_JWT_TOKEN')

def handle_tool_call(tool_name, arguments):
    """Route AI tool calls to ChartDB MCP"""
    return client.call_tool(tool_name, arguments)

# Use with Claude or other AI models
claude = anthropic.Anthropic()

message = claude.messages.create(
    model="claude-3-5-sonnet-20241022",
    tools=[{
        "name": "chartdb/create-diagram",
        "description": "Create a database diagram",
        "input_schema": {...}
    }],
    messages=[{
        "role": "user",
        "content": "Create an e-commerce database schema"
    }]
)

# Handle tool use
for content in message.content:
    if content.type == "tool_use":
        result = handle_tool_call(content.name, content.input)
        # Send result back to Claude
```

### Bulk Operations

```python
def create_schema_from_json(client, schema_def):
    """Create entire schema from JSON definition"""
    # Create diagram
    diagram = client.create_diagram(
        schema_def['name'],
        schema_def['databaseType']
    )
    
    # Create all tables
    tables = {}
    for table_def in schema_def['tables']:
        table = client.create_table(
            diagram['id'],
            **table_def
        )
        tables[table_def['name']] = table
    
    # Create all relationships
    for rel_def in schema_def['relationships']:
        client.create_relationship(
            diagram['id'],
            tables[rel_def['source']]['id'],
            tables[rel_def['target']]['id'],
            rel_def['type']
        )
    
    return diagram

# Usage
schema = {
    'name': 'Blog Schema',
    'databaseType': 'postgresql',
    'tables': [
        {'name': 'users', 'columns': [...]},
        {'name': 'posts', 'columns': [...]}
    ],
    'relationships': [
        {'source': 'posts', 'target': 'users', 'type': 'many-to-one'}
    ]
}

diagram = create_schema_from_json(client, schema)
```

## Testing

Run the comprehensive test suite:

```bash
# All tests
./test-mcp-integration.sh

# Specific endpoint
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/mcp/.well-known/mcp.json | jq
```

## Troubleshooting

### Authentication Errors

```bash
# Verify token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/diagrams
```

### Connection Refused

```bash
# Check if server is running
curl http://localhost:8080/api/v1/health
```

### Tool Not Found

```bash
# Verify MCP manifest
curl http://localhost:8080/api/mcp/.well-known/mcp.json | jq '.tools[] | .name'
```

## Contributing

Feel free to submit additional examples:

1. Fork the repository
2. Create your example
3. Add documentation
4. Submit a pull request

## Resources

- [MCP Documentation](../MCP_INTEGRATION.md)
- [ChartDB API Docs](../README.md)
- [Model Context Protocol Spec](https://modelcontextprotocol.io/)

## License

Same as ChartDB main license.
