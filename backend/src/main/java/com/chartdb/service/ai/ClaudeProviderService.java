package com.chartdb.service.ai;

import com.chartdb.dto.ai.AIRequest;
import com.chartdb.dto.ai.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeProviderService implements AIProviderService {
    
    private final ObjectMapper objectMapper;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey) {
        WebClient client = WebClient.builder()
            .baseUrl(API_URL)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        // Build request body - Claude has different format
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel() != null ? request.getModel() : "claude-3-5-sonnet-20241022");
        
        // Separate system messages from user/assistant messages
        String systemMessage = request.getMessages().stream()
            .filter(msg -> "system".equals(msg.getRole()))
            .map(com.chartdb.dto.ai.AIMessage::getContent)
            .findFirst()
            .orElse(null);
        
        if (systemMessage != null) {
            requestBody.put("system", systemMessage);
        }
        
        // Convert other messages
        var messages = request.getMessages().stream()
            .filter(msg -> !"system".equals(msg.getRole()))
            .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
            .collect(Collectors.toList());
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        
        try {
            String responseJson = client.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("content").get(0).path("text").asText();
            String model = root.path("model").asText();
            
            int inputTokens = root.path("usage").path("input_tokens").asInt();
            int outputTokens = root.path("usage").path("output_tokens").asInt();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "claude");
            metadata.put("prompt_tokens", inputTokens);
            metadata.put("completion_tokens", outputTokens);
            metadata.put("stop_reason", root.path("stop_reason").asText());
            
            return AIResponse.builder()
                .content(content)
                .model(model)
                .tokensUsed(inputTokens + outputTokens)
                .metadata(metadata)
                .build();
                
        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            throw new RuntimeException("Failed to get response from Claude: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AIProvider getProvider() {
        return AIProvider.CLAUDE;
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-ant-") && apiKey.length() > 20;
    }
}
