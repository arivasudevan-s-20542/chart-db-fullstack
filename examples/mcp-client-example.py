"""
ChartDB MCP Client Example (Python)

This example demonstrates how to integrate ChartDB MCP server
with AI agents and custom Python applications.
"""

import requests
from typing import Dict, List, Any, Optional


class ChartDBMCPClient:
    """ChartDB Model Context Protocol Client"""
    
    def __init__(self, base_url: str, token: str):
        self.base_url = base_url.rstrip('/')
        self.token = token
        self.manifest = None
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }
    
    def discover(self) -> Dict[str, Any]:
        """Discover MCP server capabilities"""
        response = requests.get(f"{self.base_url}/api/mcp/.well-known/mcp.json")
        response.raise_for_status()
        self.manifest = response.json()
        return self.manifest
    
    def call_tool(self, tool_name: str, arguments: Dict[str, Any] = None) -> Any:
        """Call an MCP tool"""
        if arguments is None:
            arguments = {}
        
        response = requests.post(
            f"{self.base_url}/api/mcp/tools/call",
            headers=self.headers,
            json={'name': tool_name, 'arguments': arguments}
        )
        response.raise_for_status()
        
        result = response.json()
        if not result.get('success'):
            raise Exception(result.get('message', 'Tool call failed'))
        
        return result.get('data')
    
    def get_resource(self, resource_uri: str) -> Any:
        """Get MCP resource"""
        response = requests.get(
            f"{self.base_url}/api/mcp/resources/{resource_uri}",
            headers=self.headers
        )
        response.raise_for_status()
        
        result = response.json()
        return result.get('data')
    
    # Diagram operations
    def create_diagram(self, name: str, database_type: str = 'postgresql') -> Dict[str, Any]:
        """Create a new diagram"""
        return self.call_tool('chartdb/create-diagram', {
            'name': name,
            'databaseType': database_type
        })
    
    def get_diagram(self, diagram_id: str) -> Dict[str, Any]:
        """Get diagram by ID"""
        return self.call_tool('chartdb/get-diagram', {'diagramId': diagram_id})
    
    def get_full_diagram(self, diagram_id: str) -> Dict[str, Any]:
        """Get full diagram with all tables, columns, and relationships"""
        return self.call_tool('chartdb/get-diagram-full', {'diagramId': diagram_id})
    
    def update_diagram(self, diagram_id: str, **updates) -> Dict[str, Any]:
        """Update diagram properties"""
        return self.call_tool('chartdb/update-diagram', {
            'diagramId': diagram_id,
            **updates
        })
    
    def delete_diagram(self, diagram_id: str) -> Dict[str, Any]:
        """Delete a diagram"""
        return self.call_tool('chartdb/delete-diagram', {'diagramId': diagram_id})
    
    def list_diagrams(self, limit: int = 10) -> List[Dict[str, Any]]:
        """List user's diagrams"""
        return self.call_tool('chartdb/list-diagrams', {'limit': limit})
    
    # Table operations
    def create_table(self, diagram_id: str, name: str, schema: str = 'public',
                    columns: List[Dict[str, Any]] = None, **kwargs) -> Dict[str, Any]:
        """Create a new table in diagram"""
        return self.call_tool('chartdb/create-table', {
            'diagramId': diagram_id,
            'name': name,
            'schema': schema,
            'columns': columns or [],
            **kwargs
        })
    
    def update_table(self, table_id: str, **updates) -> Dict[str, Any]:
        """Update table properties"""
        return self.call_tool('chartdb/update-table', {
            'tableId': table_id,
            **updates
        })
    
    def delete_table(self, table_id: str) -> Dict[str, Any]:
        """Delete a table"""
        return self.call_tool('chartdb/delete-table', {'tableId': table_id})
    
    def move_table(self, table_id: str, x: float, y: float) -> Dict[str, Any]:
        """Move table position on canvas"""
        return self.call_tool('chartdb/move-table', {
            'tableId': table_id,
            'x': x,
            'y': y
        })
    
    def list_tables(self, diagram_id: str) -> List[Dict[str, Any]]:
        """List all tables in diagram"""
        return self.call_tool('chartdb/list-tables', {'diagramId': diagram_id})
    
    # Column operations
    def create_column(self, table_id: str, name: str, column_type: str, **kwargs) -> Dict[str, Any]:
        """Add column to table"""
        return self.call_tool('chartdb/create-column', {
            'tableId': table_id,
            'name': name,
            'type': column_type,
            **kwargs
        })
    
    def update_column(self, column_id: str, **updates) -> Dict[str, Any]:
        """Update column properties"""
        return self.call_tool('chartdb/update-column', {
            'columnId': column_id,
            **updates
        })
    
    def delete_column(self, column_id: str) -> Dict[str, Any]:
        """Delete a column"""
        return self.call_tool('chartdb/delete-column', {'columnId': column_id})
    
    def reorder_columns(self, table_id: str, column_ids: List[str]) -> Dict[str, Any]:
        """Reorder columns in table"""
        return self.call_tool('chartdb/reorder-columns', {
            'tableId': table_id,
            'columnIds': column_ids
        })
    
    # Relationship operations
    def create_relationship(self, diagram_id: str, source_table_id: str,
                          target_table_id: str, relationship_type: str = 'one-to-many') -> Dict[str, Any]:
        """Create relationship between tables"""
        return self.call_tool('chartdb/create-relationship', {
            'diagramId': diagram_id,
            'sourceTableId': source_table_id,
            'targetTableId': target_table_id,
            'type': relationship_type
        })
    
    def update_relationship(self, relationship_id: str, **updates) -> Dict[str, Any]:
        """Update relationship"""
        return self.call_tool('chartdb/update-relationship', {
            'relationshipId': relationship_id,
            **updates
        })
    
    def delete_relationship(self, relationship_id: str) -> Dict[str, Any]:
        """Delete a relationship"""
        return self.call_tool('chartdb/delete-relationship', {'relationshipId': relationship_id})
    
    def list_relationships(self, diagram_id: str) -> List[Dict[str, Any]]:
        """List all relationships in diagram"""
        return self.call_tool('chartdb/list-relationships', {'diagramId': diagram_id})
    
    # Export operations
    def export_sql(self, diagram_id: str, dialect: str = 'postgresql') -> str:
        """Export diagram as SQL DDL"""
        return self.call_tool('chartdb/export-sql', {
            'diagramId': diagram_id,
            'dialect': dialect
        })
    
    def export_json(self, diagram_id: str) -> str:
        """Export diagram as JSON"""
        return self.call_tool('chartdb/export-json', {'diagramId': diagram_id})
    
    # Database connection operations
    def create_connection(self, diagram_id: str, name: str, db_type: str,
                         host: str, port: int, database: str,
                         username: str, password: str) -> Dict[str, Any]:
        """Create database connection"""
        return self.call_tool('chartdb/create-connection', {
            'diagramId': diagram_id,
            'name': name,
            'type': db_type,
            'host': host,
            'port': port,
            'database': database,
            'username': username,
            'password': password
        })
    
    def test_connection(self, host: str, port: int, database: str,
                       username: str, password: str, db_type: str = 'postgresql') -> Dict[str, Any]:
        """Test database connection"""
        return self.call_tool('chartdb/test-connection', {
            'host': host,
            'port': port,
            'database': database,
            'username': username,
            'password': password,
            'type': db_type
        })
    
    def list_connections(self, diagram_id: str) -> List[Dict[str, Any]]:
        """List database connections for diagram"""
        return self.call_tool('chartdb/list-connections', {'diagramId': diagram_id})
    
    # Query operations
    def execute_query(self, connection_id: str, query: str) -> Dict[str, Any]:
        """Execute SQL query"""
        return self.call_tool('chartdb/execute-query', {
            'connectionId': connection_id,
            'query': query
        })
    
    def get_query_history(self) -> List[Dict[str, Any]]:
        """Get query execution history"""
        return self.call_tool('chartdb/get-query-history', {})


def main():
    """Example usage"""
    # Initialize client (replace with actual token)
    client = ChartDBMCPClient('http://localhost:8080', 'YOUR_JWT_TOKEN')
    
    try:
        # 1. Discover capabilities
        print('🔍 Discovering MCP capabilities...')
        manifest = client.discover()
        print(f"✅ Found {len(manifest['tools'])} tools, "
              f"{len(manifest['resources'])} resources, "
              f"{len(manifest['prompts'])} prompts")
        
        # 2. Create a new diagram
        print('\n📊 Creating diagram...')
        diagram = client.create_diagram('E-commerce Schema', 'postgresql')
        print(f"✅ Diagram created: {diagram['id']}")
        
        # 3. Create users table
        print('\n🔨 Creating users table...')
        users_table = client.create_table(
            diagram['id'],
            name='users',
            schema='public',
            x=100,
            y=100,
            columns=[
                {'name': 'id', 'type': 'SERIAL', 'primaryKey': True},
                {'name': 'email', 'type': 'VARCHAR(255)', 'unique': True, 'nullable': False},
                {'name': 'password', 'type': 'VARCHAR(255)', 'nullable': False},
                {'name': 'created_at', 'type': 'TIMESTAMP', 'defaultValue': 'CURRENT_TIMESTAMP'}
            ]
        )
        print(f"✅ Users table created: {users_table['id']}")
        
        # 4. Create products table
        print('\n🔨 Creating products table...')
        products_table = client.create_table(
            diagram['id'],
            name='products',
            schema='public',
            x=400,
            y=100,
            columns=[
                {'name': 'id', 'type': 'SERIAL', 'primaryKey': True},
                {'name': 'name', 'type': 'VARCHAR(255)', 'nullable': False},
                {'name': 'price', 'type': 'DECIMAL(10,2)', 'nullable': False},
                {'name': 'description', 'type': 'TEXT'}
            ]
        )
        print(f"✅ Products table created: {products_table['id']}")
        
        # 5. Create orders table
        print('\n🔨 Creating orders table...')
        orders_table = client.create_table(
            diagram['id'],
            name='orders',
            schema='public',
            x=250,
            y=300,
            columns=[
                {'name': 'id', 'type': 'SERIAL', 'primaryKey': True},
                {'name': 'user_id', 'type': 'INTEGER', 'nullable': False},
                {'name': 'product_id', 'type': 'INTEGER', 'nullable': False},
                {'name': 'quantity', 'type': 'INTEGER', 'nullable': False},
                {'name': 'total_price', 'type': 'DECIMAL(10,2)', 'nullable': False},
                {'name': 'created_at', 'type': 'TIMESTAMP', 'defaultValue': 'CURRENT_TIMESTAMP'}
            ]
        )
        print(f"✅ Orders table created: {orders_table['id']}")
        
        # 6. Create relationships
        print('\n🔗 Creating relationships...')
        client.create_relationship(
            diagram['id'],
            orders_table['id'],
            users_table['id'],
            'many-to-one'
        )
        client.create_relationship(
            diagram['id'],
            orders_table['id'],
            products_table['id'],
            'many-to-one'
        )
        print('✅ Relationships created')
        
        # 7. Get full diagram
        print('\n📋 Fetching full diagram...')
        full_diagram = client.get_full_diagram(diagram['id'])
        print(f"✅ Diagram has {len(full_diagram['tables'])} tables "
              f"and {len(full_diagram['relationships'])} relationships")
        
        # 8. Export as SQL
        print('\n💾 Exporting as SQL...')
        sql = client.export_sql(diagram['id'], 'postgresql')
        print('✅ SQL generated:')
        print('---')
        print(sql)
        print('---')
        
        # 9. List all diagrams
        print('\n📋 Listing all diagrams...')
        diagrams = client.list_diagrams()
        print(f"✅ Found {len(diagrams)} diagrams")
        
        print('\n🎉 All operations completed successfully!')
        
    except Exception as e:
        print(f'❌ Error: {e}')


if __name__ == '__main__':
    main()
