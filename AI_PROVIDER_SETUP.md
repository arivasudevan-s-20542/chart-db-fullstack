# AI Provider Integration Guide

## Overview

ChartDB now supports real AI integrations with multiple providers:
- **OpenAI** (GPT-4, GPT-3.5)
- **Google Gemini** (gemini-pro, gemini-1.5-pro)
- **Anthropic Claude** (claude-3.5-sonnet, claude-3-opus)

The system uses actual API calls instead of placeholder responses, allowing you to get real AI assistance for database schema design.

## API Endpoints

### 1. Get Current Configuration
```http
GET /api/ai/config
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "config": {
      "provider": "openai",
      "apiKey": "sk-...****",
      "model": "gpt-4"
    },
    "usageStats": {
      "totalTokens": 12450,
      "totalRequests": 15,
      "lastUsed": "2024-01-09T11:10:32Z"
    },
    "configured": true
  }
}
```

### 2. Configure AI Provider
```http
POST /api/ai/config
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "provider": "openai",
  "apiKey": "sk-...",
  "model": "gpt-4"
}
```

**Supported Providers:**
- `openai` - OpenAI GPT models
- `gemini` - Google Gemini models
- `claude` - Anthropic Claude models

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "AI configuration saved successfully",
    "provider": "openai",
    "model": "gpt-4"
  }
}
```

### 3. List Available Providers
```http
GET /api/ai/config/providers
```

**Response:**
```json
{
  "success": true,
  "data": {
    "providers": [
      {
        "code": "openai",
        "name": "OpenAI",
        "models": ["gpt-4", "gpt-3.5-turbo"]
      },
      {
        "code": "gemini",
        "name": "Google Gemini",
        "models": ["gemini-pro", "gemini-1.5-pro"]
      },
      {
        "code": "claude",
        "name": "Anthropic Claude",
        "models": ["claude-3-5-sonnet-20241022", "claude-3-opus-20240229"]
      }
    ]
  }
}
```

### 4. Delete Configuration
```http
DELETE /api/ai/config
Authorization: Bearer <jwt_token>
```

## Getting API Keys

### OpenAI
1. Go to https://platform.openai.com/api-keys
2. Create a new API key
3. Copy the key (starts with `sk-`)

### Google Gemini
1. Go to https://makersuite.google.com/app/apikey
2. Create an API key
3. Copy the key

### Anthropic Claude
1. Go to https://console.anthropic.com/settings/keys
2. Create an API key
3. Copy the key (starts with `sk-ant-`)

## How It Works

1. **Configuration**: Users configure their preferred AI provider and API key via the `/api/ai/config` endpoint
2. **Storage**: Configuration is stored securely in the `user_ai_configs` table with JSONB fields
3. **API Calls**: When sending a chat message, the system:
   - Retrieves user's AI configuration
   - Builds conversation history from database
   - Adds system prompt with diagram context
   - Calls the configured AI provider's API
   - Saves the response and updates usage statistics
4. **Usage Tracking**: Tracks total tokens used, request count, and last usage time

## Example Usage Flow

1. **Configure Provider** (one-time setup):
```bash
curl -X POST http://localhost:8080/api/ai/config \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "apiKey": "sk-YOUR_OPENAI_KEY",
    "model": "gpt-4"
  }'
```

2. **Start Chat Session** (existing endpoint):
```bash
curl -X POST http://localhost:8080/api/ai/chat/start \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "diagramId": "your-diagram-id"
  }'
```

3. **Send Message** (existing endpoint, now with real AI):
```bash
curl -X POST http://localhost:8080/api/ai/chat/YOUR_SESSION_ID/message \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How should I design the relationship between users and orders?"
  }'
```

4. **Get Real AI Response**:
The system will now call the actual AI provider API and return real responses instead of placeholder text!

## Provider-Specific Details

### OpenAI
- **API Endpoint**: https://api.openai.com/v1/chat/completions
- **Authentication**: Bearer token in Authorization header
- **Format**: Standard chat completion with messages array
- **Default Model**: gpt-4
- **Features**: Supports system messages, temperature, max_tokens

### Google Gemini
- **API Endpoint**: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
- **Authentication**: API key as query parameter
- **Format**: Custom format with contents array and parts
- **Default Model**: gemini-pro
- **Note**: System messages are filtered out (not supported)

### Anthropic Claude
- **API Endpoint**: https://api.anthropic.com/v1/messages
- **Authentication**: x-api-key header
- **Format**: Messages array with separate system parameter
- **Default Model**: claude-3-5-sonnet-20241022
- **Features**: Requires max_tokens parameter, supports system messages

## Security

- API keys are stored in the database in the `config` JSONB field
- When retrieving configuration, API keys are masked (only last 4 characters shown)
- Each user has their own configuration
- API keys are never logged or exposed in error messages

## Error Handling

If an API call fails, the system will:
1. Log the error
2. Save an error message to the chat history
3. Continue functioning (won't crash)
4. Display the error message to the user

Example error response saved to chat:
```
"Sorry, I encountered an error: API key is invalid"
```

## Usage Statistics

The system tracks:
- `totalTokens`: Total tokens consumed across all requests
- `totalRequests`: Number of AI API calls made
- `lastUsed`: Timestamp of last API call

These stats are automatically updated after each successful API call and can be retrieved via GET `/api/ai/config`.

## Cost Considerations

- OpenAI GPT-4: ~$0.03 per 1K tokens (input) + $0.06 per 1K tokens (output)
- Google Gemini: Free tier available, then ~$0.0005 per 1K tokens
- Anthropic Claude: ~$0.015 per 1K tokens (input) + $0.075 per 1K tokens (output)

Monitor your usage statistics to track costs!

## Development Notes

### Architecture
- **Service Layer**: `AIProviderService` interface with 3 implementations
- **Factory Pattern**: `AIProviderFactory` for provider selection
- **DTOs**: `AIRequest`, `AIResponse`, `AIMessage` for API communication
- **WebClient**: Spring WebFlux reactive HTTP client for async API calls
- **Repository**: `UserAIConfigRepository` for configuration persistence

### Adding New Providers
1. Add provider to `AIProvider` enum
2. Create new `XxxProviderService` implementing `AIProviderService`
3. Register in `AIProviderFactory`
4. Add model information to `/providers` endpoint

### Testing
You can test the integration by:
1. Configuring an API key via the REST endpoint
2. Starting a chat session
3. Sending messages and verifying real AI responses
4. Checking usage statistics

The previous placeholder implementation returned:
```
"AI response will be generated here based on: {message}"
```

Now you get actual AI-generated responses! 🎉
