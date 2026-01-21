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
public class DeepSeekProviderService implements AIProviderService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    
    @Value("${ai.deepseek.api-key:}")
    private String defaultApiKey;
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey) {
        return sendRequest(request, apiKey, null);
    }
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey, Consumer<String> onChunk) {
        // Use default API key if none provided
        String effectiveApiKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : defaultApiKey;
        
        WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveApiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        // Build request body - DeepSeek uses OpenAI-compatible API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel() != null ? request.getModel() : "deepseek-chat");
        
        // Convert messages to DeepSeek format
        List<Map<String, String>> messages = new ArrayList<>();
        for (var message : request.getMessages()) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", message.getRole());
            msg.put("content", message.getContent());
            messages.add(msg);
        }
        requestBody.put("messages", messages);
        requestBody.put("stream", onChunk != null); // Enable streaming if callback provided
        
        // Add optional parameters
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        
        // Add tools/functions if provided (OpenAI-compatible format)
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
            log.debug("Sending request to DeepSeek API: {}/v1/chat/completions", baseUrl);
            
            if (onChunk != null) {
                // Streaming mode
                StringBuilder fullResponse = new StringBuilder();
                
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
                                        onChunk.accept(content);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse streaming chunk: {}", chunk, e);
                            }
                        }
                    })
                    .blockLast();
                
                String content = fullResponse.toString();
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("provider", "deepseek");
                metadata.put("model", request.getModel());
                metadata.put("streaming", true);
                
                return AIResponse.builder()
                    .content(content)
                    .model(request.getModel())
                    .tokensUsed(0)
                    .metadata(metadata)
                    .build();
            } else {
                // Non-streaming mode
                String responseJson = client.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Parse response
                JsonNode root = objectMapper.readTree(responseJson);
                JsonNode choices = root.path("choices");
                
                if (!choices.isArray() || choices.size() == 0) {
                    throw new RuntimeException("No choices in DeepSeek response");
                }
                
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                String model = root.path("model").asText();
                int totalTokens = root.path("usage").path("total_tokens").asInt(0);
                
                // Check for function calls
                AIFunctionCall functionCall = null;
                JsonNode toolCalls = message.path("tool_calls");
                if (toolCalls.isArray() && toolCalls.size() > 0) {
                    JsonNode firstCall = toolCalls.get(0);
                    JsonNode function = firstCall.path("function");
                    String functionName = function.path("name").asText();
                    String argumentsJson = function.path("arguments").asText();
                    
                    try {
                        Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
                        functionCall = AIFunctionCall.builder()
                            .name(functionName)
                            .arguments(arguments)
                            .build();
                    } catch (Exception e) {
                        log.warn("Failed to parse function arguments", e);
                    }
                }
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("provider", "deepseek");
                metadata.put("prompt_tokens", root.path("usage").path("prompt_tokens").asInt(0));
                metadata.put("completion_tokens", root.path("usage").path("completion_tokens").asInt(0));
                
                return AIResponse.builder()
                    .content(content)
                    .functionCall(functionCall)
                    .model(model)
                    .tokensUsed(totalTokens)
                    .metadata(metadata)
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error calling DeepSeek API", e);
            throw new RuntimeException("Failed to get response from DeepSeek: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AIProvider getProvider() {
        return AIProvider.DEEPSEEK;
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() > 20;
    }
}
