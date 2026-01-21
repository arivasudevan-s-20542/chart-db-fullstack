package com.chartdb.service;

import com.chartdb.dto.ai.*;
import com.chartdb.dto.request.ChatMessageRequest;
import com.chartdb.dto.request.StartChatSessionRequest;
import com.chartdb.dto.response.AIChatSessionResponse;
import com.chartdb.dto.response.AIMessageResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.*;
import com.chartdb.model.enums.MessageRole;
import com.chartdb.repository.*;
import com.chartdb.service.ai.AIProvider;
import com.chartdb.service.ai.AIProviderFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAssistantService {
    
    private final AIChatSessionRepository sessionRepository;
    private final AIMessageRepository messageRepository;
    private final DiagramRepository diagramRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository agentRepository;
    private final UserAIConfigRepository aiConfigRepository;
    private final DiagramService diagramService;
    private final AIProviderFactory providerFactory;
    private final DiagramActionService diagramActionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public AIChatSessionResponse startChatSession(String userId, StartChatSessionRequest request) {
        // Validate diagram exists and user has access
        Diagram diagram = diagramRepository.findById(request.getDiagramId())
            .orElseThrow(() -> new ResourceNotFoundException("Diagram not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Get agent configuration
        Map<String, Object> agentConfig = new HashMap<>();
        if (request.getAgentId() != null) {
            AIAgent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
            agentConfig = agent.getConfig();
        } else {
            // Use default schema architect agent
            agentConfig.put("name", "Schema Architect");
            agentConfig.put("model", "gpt-4");
            agentConfig.put("capabilities", List.of("schema-design", "documentation"));
        }
        
        // Build diagram context for AI
        Map<String, Object> context = buildDiagramContext(diagram);
        
        AIChatSession session = AIChatSession.builder()
            .diagram(diagram)
            .user(user)
            .agentConfig(agentConfig)
            .context(context)
            .isActive(true)
            .build();
        
        session = sessionRepository.save(session);
        log.info("Started AI chat session: {} for diagram: {}", session.getId(), diagram.getId());
        
        return mapSessionToResponse(session);
    }
    
    @Transactional
    public AIMessageResponse sendMessage(String sessionId, String userId, ChatMessageRequest request) {
        return sendMessageStreaming(sessionId, userId, request, null);
    }
    
    @Transactional
    public AIMessageResponse sendMessageStreaming(String sessionId, String userId, ChatMessageRequest request, Consumer<String> onChunk) {
        AIChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        
        if (!session.getIsActive()) {
            throw new IllegalStateException("Chat session is not active");
        }
        
        // Get user's AI configuration
        UserAIConfig aiConfig = aiConfigRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("User AI configuration not found. Please configure your AI provider settings."));
        
        Map<String, Object> config = aiConfig.getConfig();
        String providerCode = (String) config.get("provider");
        String apiKey = (String) config.get("apiKey");
        String model = (String) config.getOrDefault("model", getDefaultModel(providerCode));
        
        if (providerCode == null || apiKey == null) {
            throw new IllegalStateException("AI provider or API key not configured");
        }
        
        // Save user message
        com.chartdb.model.AIMessage userMessage = com.chartdb.model.AIMessage.builder()
            .session(session)
            .role(MessageRole.USER)
            .content(request.getMessage())
            .build();
        
        messageRepository.save(userMessage);
        
        // Update session
        session.setLastMessageAt(Instant.now());
        session.setMessageCount(session.getMessageCount() + 1);
        sessionRepository.save(session);
        
        try {
            // Get conversation history
            List<com.chartdb.model.AIMessage> history = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);
            
            // Build AI request with conversation history
            List<com.chartdb.dto.ai.AIMessage> messages = new ArrayList<>();
            
            // Add system message with context
            String systemContext = buildSystemPrompt(session.getContext());
            messages.add(com.chartdb.dto.ai.AIMessage.builder()
                .role("system")
                .content(systemContext)
                .build());
            
            // Add conversation history
            for (com.chartdb.model.AIMessage msg : history) {
                messages.add(com.chartdb.dto.ai.AIMessage.builder()
                    .role(msg.getRole().name().toLowerCase())
                    .content(msg.getContent())
                    .build());
            }
            
            AIRequest aiRequest = AIRequest.builder()
                .messages(messages)
                .model(model)
                .temperature(0.7)
                .maxTokens(2000)
                .tools(AIToolDefinitions.getDiagramTools())  // Enable agent mode with tools
                .build();
            
            // Call AI provider with retry logic for rate limiting
            AIResponse aiResponse = callAIWithRetry(providerCode, aiRequest, apiKey, onChunk);
            
            // Check if AI wants to call a function
            if (aiResponse.getFunctionCall() != null) {
                return handleFunctionCall(session, aiResponse.getFunctionCall(), 
                    userId, providerCode, apiKey, model);
            }
            
            // Save assistant response
            Map<String, Object> metadata = new HashMap<>(aiResponse.getMetadata());
            metadata.put("tokens", aiResponse.getTokensUsed());
            metadata.put("model", aiResponse.getModel());
            
            com.chartdb.model.AIMessage assistantMessage = com.chartdb.model.AIMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(aiResponse.getContent())
                .metadata(metadata)
                .build();
            
            assistantMessage = messageRepository.save(assistantMessage);
            
            session.setMessageCount(session.getMessageCount() + 1);
            sessionRepository.save(session);
            
            // Update usage stats
            updateUsageStats(aiConfig, aiResponse.getTokensUsed());
            
            return mapMessageToResponse(assistantMessage);
            
        } catch (Exception e) {
            log.error("Error getting AI response", e);
            
            // Create user-friendly error message
            String errorMessageStr = "Sorry, I encountered an error: " + e.getMessage();
            if (e.getMessage() != null && 
                (e.getMessage().contains("429") || e.getMessage().contains("Too Many Requests"))) {
                errorMessageStr = "I'm receiving too many requests right now. Please wait a moment and try again. " +
                              "If this persists, check your API rate limits or consider upgrading your plan.";
            }
            
            // Save error message
            com.chartdb.model.AIMessage errorMessage = com.chartdb.model.AIMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(errorMessageStr)
                .metadata(Map.of("error", true, "errorMessage", e.getMessage()))
                .build();
            
            errorMessage = messageRepository.save(errorMessage);
            session.setMessageCount(session.getMessageCount() + 1);
            sessionRepository.save(session);
            
            return mapMessageToResponse(errorMessage);
        }
    }
    
    private String getDefaultModel(String providerCode) {
        return switch (AIProvider.fromCode(providerCode)) {
            case OPENAI -> "gpt-4";
            case GEMINI -> "gemini-pro";
            case CLAUDE -> "claude-3-5-sonnet-20241022";
            case MISTRAL -> "mistral";
            case DEEPSEEK -> "deepseek-chat";
        };
    }
    
    /**
     * Handle AI function call - return to frontend for instant execution (Copilot-style)
     */
    @Transactional
    private AIMessageResponse handleFunctionCall(AIChatSession session, AIFunctionCall functionCall,
                                                  String userId, String providerCode, String apiKey, String model) {
        log.info("AI requested function call: {} - returning to frontend for instant execution", functionCall.getName());
        
        // Save function call as a message for audit
        Map<String, Object> functionCallMetadata = new HashMap<>();
        functionCallMetadata.put("functionName", functionCall.getName());
        functionCallMetadata.put("arguments", functionCall.getArguments());
        functionCallMetadata.put("pending", true);  // Frontend will execute and confirm
        
        com.chartdb.model.AIMessage functionCallMessage = com.chartdb.model.AIMessage.builder()
            .session(session)
            .role(MessageRole.ASSISTANT)
            .content("🤖 AI Action: " + functionCall.getName())
            .metadata(functionCallMetadata)
            .build();
        
        functionCallMessage = messageRepository.save(functionCallMessage);
        
        // Return function call to frontend WITHOUT executing
        // Frontend will apply instantly and send back confirmation
        Map<String, Object> result = Map.of(
            "success", true,
            "executeOnFrontend", true,  // Signal to frontend
            "message", "Function call ready for execution"
        );
        
        // Update metadata - frontend will execute and update later
        functionCallMetadata.put("result", result);
        functionCallMessage.setMetadata(functionCallMetadata);
        messageRepository.save(functionCallMessage);
        
        session.setMessageCount(session.getMessageCount() + 1);
        sessionRepository.save(session);
        
        return mapMessageToResponse(functionCallMessage);
    }
    
    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant helping with database schema design. ");
        prompt.append("Current diagram context:\n");
        prompt.append("- Diagram: ").append(context.get("diagramName")).append("\n");
        prompt.append("- Database Type: ").append(context.get("databaseType")).append("\n\n");
        
        // Add tables information
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) context.get("tables");
        if (tables != null && !tables.isEmpty()) {
            prompt.append("CURRENT SCHEMA:\n");
            for (Map<String, Object> table : tables) {
                prompt.append("\nTable: ").append(table.get("name")).append("\n");
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
                if (columns != null && !columns.isEmpty()) {
                    prompt.append("Columns:\n");
                    for (Map<String, Object> col : columns) {
                        prompt.append("  - ").append(col.get("name"))
                            .append(" (").append(col.get("type")).append(")");
                        if (Boolean.TRUE.equals(col.get("primaryKey"))) {
                            prompt.append(" [PK]");
                        }
                        if (Boolean.FALSE.equals(col.get("nullable"))) {
                            prompt.append(" NOT NULL");
                        }
                        if (Boolean.TRUE.equals(col.get("unique"))) {
                            prompt.append(" UNIQUE");
                        }
                        prompt.append("\n");
                    }
                }
                
                @SuppressWarnings("unchecked")
                List<String> indexes = (List<String>) table.get("indexes");
                if (indexes != null && !indexes.isEmpty()) {
                    prompt.append("Indexes: ").append(String.join(", ", indexes)).append("\n");
                }
            }
            
            // Add relationships
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> relationships = (List<Map<String, Object>>) context.get("relationships");
            if (relationships != null && !relationships.isEmpty()) {
                prompt.append("\nRELATIONSHIPS:\n");
                for (Map<String, Object> rel : relationships) {
                    prompt.append("- ").append(rel.get("from"))
                        .append(" -> ").append(rel.get("to"))
                        .append(" (").append(rel.get("type")).append(")\n");
                }
            }
        } else {
            prompt.append("\n[Empty diagram - no tables yet]\n");
        }
        
        prompt.append("\nYou can see the complete schema above. When the user asks about 'this diagram' or 'the diagram', ");
        prompt.append("you are referring to the schema shown above. Help with schema design, relationships, queries, ");
        prompt.append("optimization, and best practices. If the user asks you to review or modify the diagram, ");
        prompt.append("you can reference the specific tables and columns shown above.");
        
        return prompt.toString();
    }
    
    private void updateUsageStats(UserAIConfig config, int tokensUsed) {
        Map<String, Object> stats = config.getUsageStats();
        if (stats == null) {
            stats = new HashMap<>();
        }
        
        int totalTokens = (int) stats.getOrDefault("totalTokens", 0) + tokensUsed;
        int totalRequests = (int) stats.getOrDefault("totalRequests", 0) + 1;
        
        stats.put("totalTokens", totalTokens);
        stats.put("totalRequests", totalRequests);
        stats.put("lastUsed", Instant.now().toString());
        
        config.setUsageStats(stats);
        aiConfigRepository.save(config);
    }
    
    @Transactional(readOnly = true)
    public List<AIMessageResponse> getChatHistory(String sessionId, String userId) {
        // Verify session exists and belongs to user
        sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
            .map(this::mapMessageToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AIChatSessionResponse> getActiveSessions(String userId, String diagramId) {
        return sessionRepository.findTop10ByDiagramIdAndIsActiveTrueOrderByLastMessageAtDesc(diagramId).stream()
            .map(this::mapSessionToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void endSession(String sessionId, String userId) {
        AIChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        
        session.setIsActive(false);
        sessionRepository.save(session);
        log.info("Ended AI chat session: {}", sessionId);
    }
    
    private Map<String, Object> buildDiagramContext(Diagram diagram) {
        Map<String, Object> context = new HashMap<>();
        context.put("diagramId", diagram.getId());
        context.put("diagramName", diagram.getName());
        context.put("databaseType", diagram.getDatabaseType());
        
        // Include tables and their structure
        List<Map<String, Object>> tables = new ArrayList<>();
        for (DiagramTable table : diagram.getTables()) {
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("name", table.getName());
            
            // Include columns
            List<Map<String, Object>> columns = new ArrayList<>();
            for (TableColumn column : table.getColumns()) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("name", column.getName());
                columnInfo.put("type", column.getDataType());
                columnInfo.put("nullable", column.getIsNullable());
                columnInfo.put("primaryKey", column.getIsPrimaryKey());
                columnInfo.put("unique", column.getIsUnique());
                columns.add(columnInfo);
            }
            tableInfo.put("columns", columns);
            
            tables.add(tableInfo);
        }
        context.put("tables", tables);
        
        // Include relationships
        List<Map<String, Object>> relationships = new ArrayList<>();
        for (Relationship rel : diagram.getRelationships()) {
            Map<String, Object> relInfo = new HashMap<>();
            relInfo.put("from", rel.getSourceTable().getName());
            relInfo.put("to", rel.getTargetTable().getName());
            relInfo.put("type", rel.getRelationshipType().toString());
            relationships.add(relInfo);
        }
        context.put("relationships", relationships);
        
        return context;
    }
    
    /**
     * Call AI with exponential backoff retry for rate limiting (429 errors)
     */
    private AIResponse callAIWithRetry(String providerCode, AIRequest aiRequest, String apiKey) {
        return callAIWithRetry(providerCode, aiRequest, apiKey, null);
    }
    
    private AIResponse callAIWithRetry(String providerCode, AIRequest aiRequest, String apiKey, Consumer<String> onChunk) {
        int maxRetries = 3;
        int baseDelayMs = 1000; // Start with 1 second
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return providerFactory.getProvider(providerCode)
                    .sendRequest(aiRequest, apiKey, onChunk);
            } catch (Exception e) {
                // Check if it's a rate limiting error
                boolean isRateLimitError = e.getMessage() != null && 
                    (e.getMessage().contains("429") || 
                     e.getMessage().contains("Too Many Requests") ||
                     e.getMessage().contains("rate limit"));
                
                if (!isRateLimitError || attempt == maxRetries - 1) {
                    // Not a rate limit error, or last attempt - throw it
                    throw e;
                }
                
                // Calculate delay with exponential backoff
                int delayMs = baseDelayMs * (int) Math.pow(2, attempt);
                log.warn("Rate limit hit (attempt {}/{}), retrying in {}ms...", 
                    attempt + 1, maxRetries, delayMs);
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Failed after " + maxRetries + " retries");
    }
    
    private AIChatSessionResponse mapSessionToResponse(AIChatSession session) {
        return AIChatSessionResponse.builder()
            .id(session.getId())
            .diagramId(session.getDiagram().getId())
            .agentConfig(session.getAgentConfig())
            .messageCount(session.getMessageCount())
            .isActive(session.getIsActive())
            .startedAt(session.getStartedAt())
            .lastMessageAt(session.getLastMessageAt())
            .build();
    }
    
    private AIMessageResponse mapMessageToResponse(com.chartdb.model.AIMessage message) {
        return AIMessageResponse.builder()
            .id(message.getId())
            .role(message.getRole())
            .content(message.getContent())
            .metadata(message.getMetadata())
            .createdAt(message.getCreatedAt())
            .build();
    }
}
