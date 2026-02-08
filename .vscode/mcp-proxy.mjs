#!/usr/bin/env node
/**
 * MCP Stdio-to-HTTP proxy for ChartDB.
 * 
 * VS Code's "type: http" transport always runs OAuth discovery, which we don't support.
 * This script bridges stdio (which has NO OAuth) to our HTTP MCP endpoint,
 * sending the Bearer token with every request.
 * 
 * Usage in mcp.json:
 *   { "command": "node", "args": [".vscode/mcp-proxy.mjs"], "env": { "MCP_TOKEN": "mcp_..." } }
 */
import { createInterface } from 'readline';
import { request } from 'http';
import { request as httpsRequest } from 'https';

const MCP_URL = process.env.MCP_URL || 'http://localhost:8080/api/mcp';
const MCP_TOKEN = process.env.MCP_TOKEN || '';

const url = new URL(MCP_URL);
const isHttps = url.protocol === 'https:';
const httpRequest = isHttps ? httpsRequest : request;

let pendingRequests = 0;
let stdinClosed = false;

const rl = createInterface({ input: process.stdin, terminal: false });

rl.on('line', (line) => {
  if (!line.trim()) return;
  
  let msg;
  try {
    msg = JSON.parse(line);
  } catch {
    process.stderr.write(`[mcp-proxy] Invalid JSON: ${line}\n`);
    return;
  }

  pendingRequests++;
  const body = JSON.stringify(msg);
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Content-Length': Buffer.byteLength(body),
  };
  if (MCP_TOKEN) {
    headers['Authorization'] = `Bearer ${MCP_TOKEN}`;
  }

  const req = httpRequest({
    hostname: url.hostname,
    port: url.port || (isHttps ? 443 : 80),
    path: url.pathname,
    method: 'POST',
    headers,
  }, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
      if (data.trim()) {
        process.stdout.write(data.trim() + '\n');
      }
      pendingRequests--;
      maybeExit();
    });
  });

  req.on('error', (err) => {
    process.stderr.write(`[mcp-proxy] HTTP error: ${err.message}\n`);
    if (msg.id != null) {
      const errResp = JSON.stringify({
        jsonrpc: '2.0',
        id: msg.id,
        error: { code: -32000, message: `Server connection error: ${err.message}` }
      });
      process.stdout.write(errResp + '\n');
    }
    pendingRequests--;
    maybeExit();
  });

  req.write(body);
  req.end();
});

rl.on('close', () => {
  stdinClosed = true;
  maybeExit();
});

function maybeExit() {
  if (stdinClosed && pendingRequests === 0) {
    process.exit(0);
  }
}

process.stderr.write(`[mcp-proxy] Connected to ${MCP_URL}\n`);
