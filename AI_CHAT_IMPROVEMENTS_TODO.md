# AI Chat System - Critical Improvements Required

## Issues Identified

### 1. ❌ Message Display Issues
**Problem:** Both user and AI messages look identical - no visual differentiation
**Location:** `frontend/src/components/ai/AIChatPanel.tsx` (lines 20-52)
**Current State:**
- User messages have `bg-blue-500 text-white` but displayed on left side
- AI messages have `bg-gray-100` but also on left side
- User icon and AI icon are not clearly distinguished
- Messages are aligned using `flex-row-reverse` which causes confusion

**Fix Required:**
- User messages should be on RIGHT side with distinct styling
- AI messages should be on LEFT side with robot icon
- Better color contrast (blue for user, purple/gradient for AI)
- Add message sender label (You / AI Assistant)

### 2. ❌ Hardcoded Model Display
**Problem:** Shows "GPT-4" even when Gemini is configured
**Location:** `frontend/src/components/ai/AIChatPanel.tsx` (line 170)
```tsx
<Badge variant="outline" className="text-xs">
    <Sparkles className="mr-1 h-3 w-3" />
    GPT-4  {/* ← HARDCODED! */}
</Badge>
```

**Fix Required:**
- Fetch actual provider and model from user's AI config
- Display: "Gemini 3-flash-preview" or "Claude 3.5 Sonnet" etc.
- Update badge dynamically based on `UserAIConfig.defaultProvider` and `UserAIConfig.defaultModel`

### 3. ❌ No Message Persistence
**Problem:** Messages not saved to database, lost on page refresh
**Current State:**
- `AIMessage` type exists but no backend integration for persistence
- Store only keeps messages in memory (Zustand state)
- No database entities for storing messages

**Fix Required:**
Backend:
- Create `AIMessage` entity with JPA
- Add `AIMessageRepository`
- Implement endpoints: `GET /api/ai/sessions/{id}/messages` and `POST`

Frontend:
- Persist messages to backend after sending
- Load messages from backend on session switch
- Add loading states

### 4. ❌ No Session Management
**Problem:** Sessions exist but are not properly managed
**Current Issues:**
- Sessions load but don't show preview of last message
- No session titles - shows generic "Session"
- Clicking on conversation doesn't properly switch context
- No way to rename sessions

**Fix Required:**
- Auto-generate session titles from first user message
- Show last message preview in session list
- Implement proper session switching
- Add "Rename" option to sessions
- Show message count per session

### 5. ❌ No Multiple API Key Support
**Problem:** Only one API key per provider
**Location:** `backend/.../entity/UserAIConfig.java`
**Current Structure:**
```java
private String openaiApiKey;
private String anthropicApiKey;
```

**Fix Required:**
- Support multiple keys per provider (for rate limiting)
- Add key rotation logic
- Store keys as JSON array: `{"openai": ["key1", "key2"], "gemini": ["key3"]}`
- Implement fallback when one key fails

### 6. ❌ Agent Types Not Working
**Problem:** Multiple agent types shown but all behave the same
**Location:** `frontend/src/components/ai/NewSessionModal.tsx` and backend agent logic
**Current Issues:**
- Agents have different system prompts but not enforced
- No context-aware behavior (diagram detection)
- "Review Diagram" agent doesn't automatically detect diagram

**Fix Required:**
Backend:
- Create specialized agent handlers
- Implement diagram context injection
- Add agent-specific tools/capabilities

Frontend:
- Show agent capabilities clearly
- Add agent type indicator in chat
- Context detection (auto-select "Review Diagram" when diagram active)

### 7. ❌ Diagram Context Not Detected
**Problem:** When asking to review/edit diagram, AI asks "which diagram?"
**Root Cause:** Diagram context not passed to AI

**Fix Required:**
```typescript
// In ChatMessageRequest, automatically include:
interface ChatMessageRequest {
    content: string;
    includeContext?: boolean;  // Currently exists but not used
    // Add:
    diagramContext?: {
        diagramId: string;
        diagramName: string;
        tableCount: number;
        relationshipCount: number;
        tables: Array<{id: string, name: string, fields: string[]}>;
    };
}
```

Backend should:
- Detect diagram-related questions
- Automatically fetch diagram data
- Include in AI prompt context

### 8. ❌ Chat UI Not Modern
**Current Issues:**
- Basic styling with no animations
- No typing indicators
- No message timestamps (formatted but hidden)
- No read receipts
- No code syntax highlighting in responses
- No markdown rendering

**Fix Required:**
- Add smooth animations (slide-in for messages)
- Typing indicator with dots animation
- Show timestamps on hover
- Markdown rendering for AI responses
- Code block syntax highlighting
- Message status indicators (sending, sent, error)

### 9. ❌ UI Breaks When Clicking Conversation
**Problem:** Clicking on conversation in sessions list breaks UI
**Location:** `frontend/src/components/ai/AISessionList.tsx` (line 69-90)
**Root Cause:** Session switching doesn't properly handle state transitions

**Fix Required:**
```typescript
// In AISessionList.tsx
const handleSelectSession = (session: AIChatSession) => {
    e.stopPropagation(); // Prevent event bubbling
    onSelectSession(session);
    // Switch to chat tab
    setActiveTab('chat'); // Need to lift state up to AIAssistantPanel
};
```

Also need to:
- Clear previous messages before loading new ones
- Show loading state during session switch
- Handle errors gracefully
- Prevent multiple clicks during loading

## Implementation Priority

### 🔴 Critical (Must Fix Immediately)
1. Message Display Issues (#1)
2. Hardcoded Model Display (#2)
3. UI Breaking Issue (#9)

### 🟡 High Priority (Fix Soon)
4. Message Persistence (#3)
5. Diagram Context Detection (#7)
6. Session Management (#4)

### 🟢 Medium Priority (Nice to Have)
7. Agent Types Working (#6)
8. Modern Chat UI (#8)
9. Multiple API Keys (#5)

## Quick Fixes (Can be done now)

### Fix #1: Message Display
```tsx
// In AIChatPanel.tsx, line 20-52
const MessageBubble: React.FC<...> = ({ message, onHighlightTable }) => {
    const isUser = message.role === MessageRole.USER;
    
    return (
        <div className={`flex gap-3 mb-4 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
            {/* Icon */}
            <div className={`shrink-0 h-8 w-8 rounded-full flex items-center justify-center ${
                isUser ? 'bg-blue-600' : 'bg-gradient-to-br from-purple-500 to-pink-500'
            }`}>
                {isUser ? <User className="h-4 w-4 text-white" /> : <Bot className="h-4 w-4 text-white" />}
            </div>

            {/* Message Bubble */}
            <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} max-w-[75%]`}>
                {/* Sender Label */}
                <span className="text-xs text-gray-500 mb-1 px-2">
                    {isUser ? 'You' : 'AI Assistant'}
                </span>
                
                {/* Message Content */}
                <div className={`px-4 py-3 rounded-2xl ${
                    isUser 
                        ? 'bg-blue-600 text-white rounded-tr-md' 
                        : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-tl-md'
                }`}>
                    <p className="whitespace-pre-wrap text-sm">{message.content}</p>
                </div>
                
                {/* Timestamp */}
                <span className="text-xs text-gray-400 mt-1 px-2">
                    {formatDistanceToNow(new Date(message.createdAt), { addSuffix: true })}
                </span>
            </div>
        </div>
    );
};
```

### Fix #2: Dynamic Model Display
```tsx
// In AIChatPanel.tsx, add this near the top:
const { userConfig } = useAIAssistantStore();

// Then replace line 170:
<Badge variant="outline" className="text-xs">
    <Sparkles className="mr-1 h-3 w-3" />
    {userConfig?.defaultProvider === 'gemini' ? 'Gemini' : 
     userConfig?.defaultProvider === 'claude' ? 'Claude' : 
     userConfig?.defaultProvider === 'openai' ? 'OpenAI' : 'AI'} 
    {' • '}
    {userConfig?.defaultModel || 'gpt-4'}
</Badge>
```

Also need to load config in store:
```typescript
// In ai-assistant.store.ts, add:
loadConfig: async () => {
    set({ isLoadingConfig: true });
    try {
        const config = await aiApi.getCurrentConfig();
        set({ userConfig: config, isLoadingConfig: false });
    } catch (error) {
        set({ isLoadingConfig: false });
    }
},
```

### Fix #9: Session Switching
```tsx
// In AISessionList.tsx
onClick={(e) => {
    e.stopPropagation();
    onSelectSession(session);
}}
```

And in AIAssistantPanel.tsx:
```tsx
const handleSelectSession = (session: AIChatSession) => {
    setActiveSession(session.id);
    // Force switch to chat tab
    const chatTab = document.querySelector('[value="chat"]') as HTMLElement;
    chatTab?.click();
};
```

## Testing Checklist

After implementing fixes:
- [ ] User messages appear on right side
- [ ] AI messages appear on left side  
- [ ] Visual distinction between user/AI is clear
- [ ] Model badge shows correct provider (Gemini/OpenAI/Claude)
- [ ] Model badge shows correct model name
- [ ] Messages persist after page refresh
- [ ] Clicking conversation switches properly without breaking
- [ ] Session list shows last message preview
- [ ] Agent types have different behaviors
- [ ] Asking about diagram doesn't require specifying which one
- [ ] UI animations are smooth
- [ ] No console errors when switching sessions
- [ ] Multiple API keys can be configured
- [ ] Code blocks in AI responses are properly formatted

## Next Steps

1. **Immediate Action:**
   - Fix message display (visual distinction)
   - Fix hardcoded GPT-4 display
   - Fix session switching bug

2. **Short Term (This Week):**
   - Implement message persistence
   - Add diagram context detection
   - Improve session management

3. **Medium Term (Next 2 Weeks):**
   - Agent specialization
   - Modern UI improvements
   - Multiple API key support

---

**Last Updated:** 2026-01-10  
**Status:** 🔴 Critical Issues Present  
**Estimated Fix Time:** 6-8 hours for critical fixes
