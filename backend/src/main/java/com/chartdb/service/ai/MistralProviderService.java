package com.chartdb.service.ai;

import com.chartdb.dto.ai.AIFunctionCall;
import com.chartdb.dto.ai.AIRequest;
import com.chartdb.dto.ai.AIResponse;
import com.chartdb.dto.ai.AITool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistralProviderService implements AIProviderService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${mistral.base-url:https://mistral-ai.chartdb.in}")
    private String baseUrl;
    
    @Override
    public AIProvider getProvider() {
        return AIProvider.MISTRAL;
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        // Mistral API key should be a non-empty string (64 hex characters from your example)
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() >= 32;
    }
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey) {
        return sendRequest(request, apiKey, null);
    }
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey, Consumer<String> onChunk) {
        WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        // Build request body for Mistral Chat API format
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel() != null ? request.getModel().replace("mistral-", "") : "mistral-small-latest");
        
        // Convert messages to Mistral format
        List<Map<String, String>> messages = new ArrayList<>();
        for (var message : request.getMessages()) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", message.getRole());
            msg.put("content", message.getContent());
            messages.add(msg);
        }
        requestBody.put("messages", messages);
        requestBody.put("stream", true); // Enable streaming for faster response
        
        // Add optional parameters
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        
        // Add tools/functions if provided
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (AITool tool : request.getTools()) {
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("type", "function");
                Map<String, Object> function = new HashMap<>();
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.put("parameters", tool.getParameters());
                toolDef.put("function", function);
                tools.add(toolDef);
            }
            requestBody.put("tools", tools);
            requestBody.put("tool_choice", "auto");
        }
        
        try {
            log.debug("Sending request to Mistral API: {}/v1/chat/completions", baseUrl);
            
            StringBuilder fullResponse = new StringBuilder();
            
            // Use streaming for faster token-by-token response
            client.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    // Process each SSE chunk
                    if (chunk.startsWith("data: ") && !chunk.contains("[DONE]")) {
                        try {
                            String jsonData = chunk.substring(6).trim();
                            JsonNode chunkNode = objectMapper.readTree(jsonData);
                            JsonNode choices = chunkNode.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta");
                                String content = delta.path("content").asText("");
                                if (!content.isEmpty()) {
                                    fullResponse.append(content);
                                    // Stream chunk to frontend if callback provided
                                    if (onChunk != null) {
                                        onChunk.accept(content);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse streaming chunk: {}", chunk, e);
                        }
                    }
                })
                .blockLast(); // Wait for all chunks
            
            String content = fullResponse.toString();
            
            // Check if response contains function call
            AIFunctionCall functionCall = null;
            if (content.contains("\"function_call\"") || content.contains("\"tool_calls\"")) {
                functionCall = extractFunctionCall(content);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "mistral");
            metadata.put("model", request.getModel());
            metadata.put("streaming", true);
            
            return AIResponse.builder()
                .content(content)
                .functionCall(functionCall)
                .model(request.getModel())
                .tokensUsed(0) // Token counting not available in streaming mode
                .metadata(metadata)
                .build();
            
        } catch (Exception e) {
            log.error("Error calling Mistral API", e);
            throw new RuntimeException("Failed to get response from Mistral: " + e.getMessage(), e);
        }
    }
    
    private AIFunctionCall extractFunctionCall(String content) {
        try {
            // Try to extract JSON from content
            int startIdx = content.indexOf("{");
            int endIdx = content.lastIndexOf("}") + 1;
            
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonStr = content.substring(startIdx, endIdx);
                JsonNode json = objectMapper.readTree(jsonStr);
                
                if (json.has("function_call")) {
                    JsonNode fcNode = json.get("function_call");
                    String name = fcNode.path("name").asText();
                    JsonNode args = fcNode.path("arguments");
                    
                    return AIFunctionCall.builder()
                        .name(name)
                        .arguments(objectMapper.convertValue(args, Map.class))
                        .build();
                } else if (json.has("tool_calls")) {
                    JsonNode toolCalls = json.get("tool_calls");
                    if (toolCalls.isArray() && toolCalls.size() > 0) {
                        JsonNode firstCall = toolCalls.get(0);
                        JsonNode function = firstCall.path("function");
                        String name = function.path("name").asText();
                        String argsStr = function.path("arguments").asText();
                        
                        Map<String, Object> args = objectMapper.readValue(argsStr, Map.class);
                        return AIFunctionCall.builder()
                            .name(name)
                            .arguments(args)
                            .build();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract function call from content", e);
        }
        return null;
    }
}
