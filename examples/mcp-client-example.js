/**
 * ChartDB MCP Client Example (Node.js)
 * 
 * This example demonstrates how to integrate ChartDB MCP server
 * with AI agents and custom applications.
 */

class ChartDBMCPClient {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
    this.manifest = null;
  }

  /**
   * Discover MCP server capabilities
   */
  async discover() {
    const response = await fetch(`${this.baseUrl}/api/mcp/.well-known/mcp.json`);
    this.manifest = await response.json();
    return this.manifest;
  }

  /**
   * Call an MCP tool
   */
  async callTool(toolName, arguments = {}) {
    const response = await fetch(`${this.baseUrl}/api/mcp/tools/call`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: toolName, arguments })
    });

    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message || 'Tool call failed');
    }
    
    return result.data;
  }

  /**
   * Get MCP resource
   */
  async getResource(resourceUri) {
    const response = await fetch(`${this.baseUrl}/api/mcp/resources/${resourceUri}`, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    const result = await response.json();
    return result.data;
  }

  // Diagram operations
  async createDiagram(name, databaseType = 'postgresql') {
    return this.callTool('chartdb/create-diagram', { name, databaseType });
  }

  async getDiagram(diagramId) {
    return this.callTool('chartdb/get-diagram', { diagramId });
  }

  async getFullDiagram(diagramId) {
    return this.callTool('chartdb/get-diagram-full', { diagramId });
  }

  async updateDiagram(diagramId, updates) {
    return this.callTool('chartdb/update-diagram', { diagramId, ...updates });
  }

  async deleteDiagram(diagramId) {
    return this.callTool('chartdb/delete-diagram', { diagramId });
  }

  async listDiagrams(limit = 10) {
    return this.callTool('chartdb/list-diagrams', { limit });
  }

  // Table operations
  async createTable(diagramId, tableData) {
    return this.callTool('chartdb/create-table', { diagramId, ...tableData });
  }

  async updateTable(tableId, updates) {
    return this.callTool('chartdb/update-table', { tableId, ...updates });
  }

  async deleteTable(tableId) {
    return this.callTool('chartdb/delete-table', { tableId });
  }

  async moveTable(tableId, x, y) {
    return this.callTool('chartdb/move-table', { tableId, x, y });
  }

  async listTables(diagramId) {
    return this.callTool('chartdb/list-tables', { diagramId });
  }

  // Column operations
  async createColumn(tableId, columnData) {
    return this.callTool('chartdb/create-column', { tableId, ...columnData });
  }

  async updateColumn(columnId, updates) {
    return this.callTool('chartdb/update-column', { columnId, ...updates });
  }

  async deleteColumn(columnId) {
    return this.callTool('chartdb/delete-column', { columnId });
  }

  async reorderColumns(tableId, columnIds) {
    return this.callTool('chartdb/reorder-columns', { tableId, columnIds });
  }

  // Relationship operations
  async createRelationship(diagramId, relationshipData) {
    return this.callTool('chartdb/create-relationship', { diagramId, ...relationshipData });
  }

  async updateRelationship(relationshipId, updates) {
    return this.callTool('chartdb/update-relationship', { relationshipId, ...updates });
  }

  async deleteRelationship(relationshipId) {
    return this.callTool('chartdb/delete-relationship', { relationshipId });
  }

  async listRelationships(diagramId) {
    return this.callTool('chartdb/list-relationships', { diagramId });
  }

  // Export operations
  async exportSQL(diagramId, dialect = 'postgresql') {
    return this.callTool('chartdb/export-sql', { diagramId, dialect });
  }

  async exportJSON(diagramId) {
    return this.callTool('chartdb/export-json', { diagramId });
  }

  // Database connection operations
  async createConnection(connectionData) {
    return this.callTool('chartdb/create-connection', connectionData);
  }

  async testConnection(connectionData) {
    return this.callTool('chartdb/test-connection', connectionData);
  }

  async listConnections(diagramId) {
    return this.callTool('chartdb/list-connections', { diagramId });
  }

  // Query operations
  async executeQuery(connectionId, query) {
    return this.callTool('chartdb/execute-query', { connectionId, query });
  }

  async getQueryHistory() {
    return this.callTool('chartdb/get-query-history', {});
  }
}

// Example usage
async function main() {
  // Initialize client (replace with actual token)
  const client = new ChartDBMCPClient('http://localhost:8080', 'YOUR_JWT_TOKEN');

  try {
    // 1. Discover capabilities
    console.log('🔍 Discovering MCP capabilities...');
    const manifest = await client.discover();
    console.log(`✅ Found ${manifest.tools.length} tools, ${manifest.resources.length} resources, ${manifest.prompts.length} prompts`);

    // 2. Create a new diagram
    console.log('\n📊 Creating diagram...');
    const diagram = await client.createDiagram('E-commerce Schema', 'postgresql');
    console.log(`✅ Diagram created: ${diagram.id}`);

    // 3. Create users table
    console.log('\n🔨 Creating users table...');
    const usersTable = await client.createTable(diagram.id, {
      name: 'users',
      schema: 'public',
      x: 100,
      y: 100,
      columns: [
        { name: 'id', type: 'SERIAL', primaryKey: true },
        { name: 'email', type: 'VARCHAR(255)', unique: true, nullable: false },
        { name: 'password', type: 'VARCHAR(255)', nullable: false },
        { name: 'created_at', type: 'TIMESTAMP', defaultValue: 'CURRENT_TIMESTAMP' }
      ]
    });
    console.log(`✅ Users table created: ${usersTable.id}`);

    // 4. Create products table
    console.log('\n🔨 Creating products table...');
    const productsTable = await client.createTable(diagram.id, {
      name: 'products',
      schema: 'public',
      x: 400,
      y: 100,
      columns: [
        { name: 'id', type: 'SERIAL', primaryKey: true },
        { name: 'name', type: 'VARCHAR(255)', nullable: false },
        { name: 'price', type: 'DECIMAL(10,2)', nullable: false },
        { name: 'description', type: 'TEXT' }
      ]
    });
    console.log(`✅ Products table created: ${productsTable.id}`);

    // 5. Create orders table
    console.log('\n🔨 Creating orders table...');
    const ordersTable = await client.createTable(diagram.id, {
      name: 'orders',
      schema: 'public',
      x: 250,
      y: 300,
      columns: [
        { name: 'id', type: 'SERIAL', primaryKey: true },
        { name: 'user_id', type: 'INTEGER', nullable: false },
        { name: 'product_id', type: 'INTEGER', nullable: false },
        { name: 'quantity', type: 'INTEGER', nullable: false },
        { name: 'total_price', type: 'DECIMAL(10,2)', nullable: false },
        { name: 'created_at', type: 'TIMESTAMP', defaultValue: 'CURRENT_TIMESTAMP' }
      ]
    });
    console.log(`✅ Orders table created: ${ordersTable.id}`);

    // 6. Create relationships
    console.log('\n🔗 Creating relationships...');
    await client.createRelationship(diagram.id, {
      sourceTableId: ordersTable.id,
      targetTableId: usersTable.id,
      type: 'many-to-one'
    });
    await client.createRelationship(diagram.id, {
      sourceTableId: ordersTable.id,
      targetTableId: productsTable.id,
      type: 'many-to-one'
    });
    console.log('✅ Relationships created');

    // 7. Get full diagram
    console.log('\n📋 Fetching full diagram...');
    const fullDiagram = await client.getFullDiagram(diagram.id);
    console.log(`✅ Diagram has ${fullDiagram.tables.length} tables and ${fullDiagram.relationships.length} relationships`);

    // 8. Export as SQL
    console.log('\n💾 Exporting as SQL...');
    const sql = await client.exportSQL(diagram.id, 'postgresql');
    console.log('✅ SQL generated:');
    console.log('---');
    console.log(sql);
    console.log('---');

    // 9. List all diagrams
    console.log('\n📋 Listing all diagrams...');
    const diagrams = await client.listDiagrams();
    console.log(`✅ Found ${diagrams.length} diagrams`);

    console.log('\n🎉 All operations completed successfully!');
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

// Uncomment to run
// main();

module.exports = { ChartDBMCPClient };
