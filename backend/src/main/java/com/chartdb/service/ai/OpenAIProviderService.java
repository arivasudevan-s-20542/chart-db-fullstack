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
public class OpenAIProviderService implements AIProviderService {
    
    private final ObjectMapper objectMapper;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    @Override
    public AIResponse sendRequest(AIRequest request, String apiKey) {
        WebClient client = WebClient.builder()
            .baseUrl(API_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel() != null ? request.getModel() : "gpt-4");
        requestBody.put("messages", request.getMessages().stream()
            .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
            .collect(Collectors.toList()));
        
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        
        try {
            String responseJson = client.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            String model = root.path("model").asText();
            int totalTokens = root.path("usage").path("total_tokens").asInt();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "openai");
            metadata.put("prompt_tokens", root.path("usage").path("prompt_tokens").asInt());
            metadata.put("completion_tokens", root.path("usage").path("completion_tokens").asInt());
            
            return AIResponse.builder()
                .content(content)
                .model(model)
                .tokensUsed(totalTokens)
                .metadata(metadata)
                .build();
                
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to get response from OpenAI: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AIProvider getProvider() {
        return AIProvider.OPENAI;
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() > 20;
    }
}
