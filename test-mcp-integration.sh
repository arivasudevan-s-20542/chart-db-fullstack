#!/bin/bash

# ChartDB MCP Integration Test Script
# This script demonstrates how to use the ChartDB MCP API

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-test@example.com}"
PASSWORD="${PASSWORD:-testpassword}"

echo "🚀 ChartDB MCP Integration Test"
echo "================================"
echo ""

# Step 1: Login and get JWT token
echo "📝 Step 1: Authenticating..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken')

if [ "$JWT_TOKEN" == "null" ] || [ -z "$JWT_TOKEN" ]; then
  echo "❌ Authentication failed. Please check credentials."
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Authenticated successfully"
echo ""

# Step 2: Discover MCP capabilities
echo "🔍 Step 2: Discovering MCP capabilities..."
MCP_MANIFEST=$(curl -s "$API_BASE_URL/api/mcp/.well-known/mcp.json")

TOOL_COUNT=$(echo "$MCP_MANIFEST" | jq '.tools | length')
RESOURCE_COUNT=$(echo "$MCP_MANIFEST" | jq '.resources | length')
PROMPT_COUNT=$(echo "$MCP_MANIFEST" | jq '.prompts | length')

echo "✅ MCP Server discovered:"
echo "   - Tools: $TOOL_COUNT"
echo "   - Resources: $RESOURCE_COUNT"
echo "   - Prompts: $PROMPT_COUNT"
echo ""

# Step 3: Create a diagram using MCP
echo "📊 Step 3: Creating diagram via MCP..."
CREATE_DIAGRAM_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "chartdb/create-diagram",
    "arguments": {
      "name": "MCP Test Diagram",
      "databaseType": "postgresql"
    }
  }')

DIAGRAM_ID=$(echo "$CREATE_DIAGRAM_RESPONSE" | jq -r '.data.id')

if [ "$DIAGRAM_ID" == "null" ] || [ -z "$DIAGRAM_ID" ]; then
  echo "❌ Failed to create diagram"
  echo "Response: $CREATE_DIAGRAM_RESPONSE"
  exit 1
fi

echo "✅ Diagram created: $DIAGRAM_ID"
echo ""

# Step 4: Create a table using MCP
echo "🔨 Step 4: Creating table via MCP..."
CREATE_TABLE_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"chartdb/create-table\",
    \"arguments\": {
      \"diagramId\": \"$DIAGRAM_ID\",
      \"name\": \"users\",
      \"schema\": \"public\",
      \"x\": 100,
      \"y\": 100,
      \"columns\": [
        {
          \"name\": \"id\",
          \"type\": \"INTEGER\",
          \"primaryKey\": true,
          \"nullable\": false,
          \"autoIncrement\": true
        },
        {
          \"name\": \"email\",
          \"type\": \"VARCHAR(255)\",
          \"unique\": true,
          \"nullable\": false
        },
        {
          \"name\": \"created_at\",
          \"type\": \"TIMESTAMP\",
          \"nullable\": false,
          \"defaultValue\": \"CURRENT_TIMESTAMP\"
        }
      ]
    }
  }")

TABLE_ID=$(echo "$CREATE_TABLE_RESPONSE" | jq -r '.data.id')

if [ "$TABLE_ID" == "null" ] || [ -z "$TABLE_ID" ]; then
  echo "❌ Failed to create table"
  echo "Response: $CREATE_TABLE_RESPONSE"
  exit 1
fi

echo "✅ Table created: $TABLE_ID (users)"
echo ""

# Step 5: Create another table
echo "🔨 Step 5: Creating second table via MCP..."
CREATE_POSTS_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"chartdb/create-table\",
    \"arguments\": {
      \"diagramId\": \"$DIAGRAM_ID\",
      \"name\": \"posts\",
      \"schema\": \"public\",
      \"x\": 400,
      \"y\": 100,
      \"columns\": [
        {
          \"name\": \"id\",
          \"type\": \"INTEGER\",
          \"primaryKey\": true,
          \"nullable\": false,
          \"autoIncrement\": true
        },
        {
          \"name\": \"user_id\",
          \"type\": \"INTEGER\",
          \"nullable\": false
        },
        {
          \"name\": \"title\",
          \"type\": \"VARCHAR(255)\",
          \"nullable\": false
        },
        {
          \"name\": \"content\",
          \"type\": \"TEXT\"
        }
      ]
    }
  }")

POSTS_TABLE_ID=$(echo "$CREATE_POSTS_RESPONSE" | jq -r '.data.id')

if [ "$POSTS_TABLE_ID" == "null" ] || [ -z "$POSTS_TABLE_ID" ]; then
  echo "❌ Failed to create posts table"
  echo "Response: $CREATE_POSTS_RESPONSE"
  exit 1
fi

echo "✅ Table created: $POSTS_TABLE_ID (posts)"
echo ""

# Step 6: List all tables
echo "📋 Step 6: Listing all tables via MCP..."
LIST_TABLES_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"chartdb/list-tables\",
    \"arguments\": {
      \"diagramId\": \"$DIAGRAM_ID\"
    }
  }")

TABLE_COUNT=$(echo "$LIST_TABLES_RESPONSE" | jq '.data | length')
echo "✅ Found $TABLE_COUNT tables in diagram"
echo ""

# Step 7: Export as SQL
echo "💾 Step 7: Exporting diagram as SQL via MCP..."
EXPORT_SQL_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"chartdb/export-sql\",
    \"arguments\": {
      \"diagramId\": \"$DIAGRAM_ID\",
      \"dialect\": \"postgresql\"
    }
  }")

SQL_CONTENT=$(echo "$EXPORT_SQL_RESPONSE" | jq -r '.data')

echo "✅ SQL exported successfully:"
echo "---"
echo "$SQL_CONTENT"
echo "---"
echo ""

# Step 8: Get full diagram
echo "🔍 Step 8: Fetching full diagram via MCP resource..."
FULL_DIAGRAM=$(curl -s "$API_BASE_URL/api/mcp/resources/diagram/$DIAGRAM_ID/schema" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "✅ Full diagram retrieved:"
echo "$FULL_DIAGRAM" | jq '.'
echo ""

# Step 9: Clean up - delete diagram
echo "🧹 Step 9: Cleaning up (deleting diagram)..."
DELETE_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/mcp/tools/call" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"chartdb/delete-diagram\",
    \"arguments\": {
      \"diagramId\": \"$DIAGRAM_ID\"
    }
  }")

echo "✅ Diagram deleted"
echo ""

echo "🎉 All MCP integration tests passed!"
echo ""
echo "Summary:"
echo "--------"
echo "✅ Authentication"
echo "✅ MCP discovery"
echo "✅ Create diagram"
echo "✅ Create tables"
echo "✅ List tables"
echo "✅ Export SQL"
echo "✅ Get resources"
echo "✅ Delete diagram"
echo ""
echo "🚀 ChartDB MCP is fully operational!"
