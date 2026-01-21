package com.chartdb.service.ai;

import com.chartdb.dto.ai.AIFunctionCall;
import com.chartdb.dto.ai.AIRequest;
import com.chartdb.dto.ai.AIResponse;
import com.chartdb.dto.ai.AITool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiProviderService implements AIProviderService {
    
    private final ObjectMapper objectMapper;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey) {
        String model = request.getModel() != null ? request.getModel() : "gemini-1.5-flash";
        
        // Map common model names to Gemini API model names
        String apiModel = mapToGeminiModel(model);
        
        WebClient client = WebClient.builder()
            .baseUrl(API_URL + apiModel + ":generateContent")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        // Build request body - Gemini has different format
        Map<String, Object> requestBody = new HashMap<>();
        
        // Convert messages to Gemini format
        var contents = request.getMessages().stream()
            .filter(msg -> !"system".equals(msg.getRole())) // Gemini doesn't support system messages
            .map(msg -> {
                Map<String, Object> part = new HashMap<>();
                part.put("text", msg.getContent());
                Map<String, Object> content = new HashMap<>();
                content.put("role", "user".equals(msg.getRole()) ? "user" : "model");
                content.put("parts", new Object[]{part});
                return content;
            })
            .collect(Collectors.toList());
        
        requestBody.put("contents", contents);
        
        // Add tools/functions if provided
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            Map<String, Object> toolWrapper = new HashMap<>();
            List<Map<String, Object>> functionDeclarations = request.getTools().stream()
                .map(this::convertToolToGeminiFormat)
                .collect(Collectors.toList());
            toolWrapper.put("functionDeclarations", functionDeclarations);
            tools.add(toolWrapper);
            requestBody.put("tools", tools);
        }
        
        if (request.getTemperature() != null || request.getMaxTokens() != null) {
            Map<String, Object> generationConfig = new HashMap<>();
            if (request.getTemperature() != null) {
                generationConfig.put("temperature", request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                generationConfig.put("maxOutputTokens", request.getMaxTokens());
            }
            requestBody.put("generationConfig", generationConfig);
        }
        
        try {
            String responseJson = client.post()
                .header("x-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidate = root.path("candidates").get(0);
            JsonNode content = candidate.path("content");
            JsonNode parts = content.path("parts");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "gemini");
            
            // Check if response contains a function call
            if (parts.size() > 0 && parts.get(0).has("functionCall")) {
                JsonNode functionCallNode = parts.get(0).path("functionCall");
                String functionName = functionCallNode.path("name").asText();
                
                // Parse function arguments
                Map<String, Object> arguments = new HashMap<>();
                JsonNode argsNode = functionCallNode.path("args");
                argsNode.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();
                    if (value.isTextual()) {
                        arguments.put(entry.getKey(), value.asText());
                    } else if (value.isBoolean()) {
                        arguments.put(entry.getKey(), value.asBoolean());
                    } else if (value.isNumber()) {
                        arguments.put(entry.getKey(), value.asDouble());
                    } else if (value.isArray()) {
                        List<Object> list = new ArrayList<>();
                        value.forEach(item -> {
                            if (item.isTextual()) list.add(item.asText());
                            else if (item.isObject()) list.add(objectMapper.convertValue(item, Map.class));
                        });
                        arguments.put(entry.getKey(), list);
                    } else if (value.isObject()) {
                        arguments.put(entry.getKey(), objectMapper.convertValue(value, Map.class));
                    }
                });
                
                AIFunctionCall functionCall = AIFunctionCall.builder()
                    .name(functionName)
                    .arguments(arguments)
                    .build();
                
                return AIResponse.builder()
                    .content(null)
                    .model(model)
                    .functionCall(functionCall)
                    .tokensUsed(root.has("usageMetadata") ? root.path("usageMetadata").path("totalTokenCount").asInt() : 0)
                    .metadata(metadata)
                    .build();
            }
            
            // Normal text response
            String textContent = parts.get(0).path("text").asText();
            
            // Gemini may not return token counts in all cases
            if (root.has("usageMetadata")) {
                int totalTokens = root.path("usageMetadata").path("totalTokenCount").asInt();
                metadata.put("prompt_tokens", root.path("usageMetadata").path("promptTokenCount").asInt());
                metadata.put("completion_tokens", root.path("usageMetadata").path("candidatesTokenCount").asInt());
                
                return AIResponse.builder()
                    .content(textContent)
                    .model(model)
                    .tokensUsed(totalTokens)
                    .metadata(metadata)
                    .build();
            }
            
            return AIResponse.builder()
                .content(textContent)
                .model(model)
                .tokensUsed(0)
                .metadata(metadata)
                .build();
                
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to get response from Gemini: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AIProvider getProvider() {
        return AIProvider.GEMINI;
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.length() > 20;
    }
    
    /**
     * Map common model names to actual Gemini API model names
     * Valid models: gemini-1.5-pro, gemini-1.5-flash, gemini-1.5-flash-8b, gemini-3-flash-preview
     * Note: gemini-pro (legacy) is redirected to gemini-1.5-flash
     */
    private String mapToGeminiModel(String model) {
        if (model == null) {
            return "gemini-1.5-flash";
        }
        
        // Map various model name formats to actual Gemini API model names
        // Remove any -latest suffix as Gemini API doesn't use it
        return switch (model.toLowerCase()) {
            case "gemini-1.5-pro", "gemini-1.5-pro-latest" -> "gemini-1.5-pro";
            case "gemini-1.5-flash", "gemini-1.5-flash-latest" -> "gemini-1.5-flash";
            case "gemini-1.5-flash-8b" -> "gemini-1.5-flash-8b";
            // Gemini 3 preview models
            case "gemini-3-flash-preview" -> "gemini-3-flash-preview";
            case "gemini-3-pro-preview" -> "gemini-3-pro-preview";
            // Legacy models (deprecated) - redirect to modern equivalents
            case "gemini-pro", "gemini-pro-vision" -> "gemini-1.5-flash";
            default -> model; // Use as-is if not recognized
        };
    }
    
    /**
     * Convert AITool to Gemini function declaration format
     */
    private Map<String, Object> convertToolToGeminiFormat(AITool tool) {
        Map<String, Object> functionDeclaration = new HashMap<>();
        functionDeclaration.put("name", tool.getName());
        functionDeclaration.put("description", tool.getDescription());
        functionDeclaration.put("parameters", tool.getParameters());
        return functionDeclaration;
    }
}
