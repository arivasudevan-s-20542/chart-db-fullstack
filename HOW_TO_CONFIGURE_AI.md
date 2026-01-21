# How to Configure AI Provider

## Quick Start Guide

Your ChartDB now has a **Settings page** where you can configure your AI assistant!

## Step-by-Step Instructions

### 1. Access Settings

**Option A: From the User Menu**
1. Click on your **avatar/profile icon** in the top-right corner
2. Click **"Settings"** from the dropdown menu

**Option B: Direct URL**
- Navigate to: `http://localhost:5173/settings`

### 2. Choose Your AI Provider

You have three options:
- **OpenAI** (GPT-4, GPT-3.5-turbo)
- **Google Gemini** (gemini-pro, gemini-1.5-pro)
- **Anthropic Claude** (claude-3.5-sonnet, claude-3-opus)

### 3. Get Your API Key

#### For OpenAI:
1. Visit: https://platform.openai.com/api-keys
2. Sign in or create an account
3. Click "Create new secret key"
4. Copy the key (starts with `sk-`)

#### For Google Gemini:
1. Visit: https://makersuite.google.com/app/apikey
2. Sign in with your Google account
3. Click "Create API key"
4. Copy the key

#### For Anthropic Claude:
1. Visit: https://console.anthropic.com/settings/keys
2. Sign in or create an account
3. Click "Create Key"
4. Copy the key (starts with `sk-ant-`)

### 4. Configure in Settings Page

1. **Select Provider**: Choose your AI provider from the dropdown
2. **Enter API Key**: Paste your API key
   - Click the eye icon to show/hide the key
   - The key will be masked when you reload the page
3. **Select Model**: Choose which model to use
   - OpenAI: gpt-4, gpt-3.5-turbo, gpt-4-turbo
   - Gemini: gemini-pro, gemini-1.5-pro
   - Claude: claude-3-5-sonnet, claude-3-opus, claude-3-sonnet
4. Click **"Save Configuration"**

### 5. Start Using AI Assistant

Once configured:
1. Open any diagram
2. Click the AI assistant icon
3. Start chatting with your AI assistant
4. You'll now get **real AI responses** instead of placeholder text!

## Settings Page Features

### Configuration Section
- **Provider Selection**: Dropdown to choose AI provider
- **API Key Input**: Secure input with show/hide toggle
- **Model Selection**: Choose from available models
- **Save Button**: Saves your configuration
- **Delete Button**: Removes configuration (if configured)

### Usage Statistics
After you start using the AI assistant, you'll see:
- **Total Tokens**: How many tokens you've used
- **Total Requests**: Number of AI API calls made
- **Last Used**: When you last used the AI assistant

## Security Notes

✅ **Your API key is secure:**
- Stored in your user account
- Masked when displayed (only last 4 characters shown)
- Never logged or exposed in error messages
- Transmitted securely via HTTPS

## Troubleshooting

### "User AI configuration not found" Error
**Solution**: You need to configure your AI provider first in Settings

### API Key Doesn't Work
**Solution**: 
- Verify the key is copied correctly (no extra spaces)
- Check if you have credits/quota remaining with the provider
- Ensure the key is for the correct provider (OpenAI keys won't work for Gemini, etc.)

### Can't Find Settings Page
**Solution**:
- Make sure you're logged in
- Click your avatar in the top-right corner
- Select "Settings" from the menu

## Cost Information

Be aware of API costs:

- **OpenAI GPT-4**: ~$0.03 per 1K input tokens, $0.06 per 1K output tokens
- **Google Gemini**: Free tier available, then ~$0.0005 per 1K tokens
- **Anthropic Claude**: ~$0.015 per 1K input tokens, $0.075 per 1K output tokens

Monitor your usage statistics on the Settings page!

## What Happens Next?

Once configured, every time you chat with the AI assistant:
1. The system retrieves your configuration
2. Builds conversation history with diagram context
3. Calls your chosen AI provider's API
4. Returns real, intelligent responses
5. Updates your usage statistics

No more placeholder text - **you get real AI assistance!** 🎉
