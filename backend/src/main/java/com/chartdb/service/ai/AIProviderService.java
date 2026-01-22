package com.chartdb.service.ai;

import com.chartdb.dto.ai.AIRequest;
import com.chartdb.dto.ai.AIResponse;

import java.util.function.Consumer;

/**
 * Interface for AI provider implementations
 */
public interface AIProviderService {
    
    /**
     * Send a request to the AI provider and get a response
     */
    AIResponse sendRequest(AIRequest request, String apiKey);
    
    /**
     * Send a request with streaming support
     */
    default AIResponse sendRequest(AIRequest request, String apiKey, Consumer<String> onChunk) {
        return sendRequest(request, apiKey);
    }
    
    /**
     * Get the provider type this service handles
     */
    AIProvider getProvider();
    
    /**
     * Validate if the API key is in the correct format
     */
    boolean validateApiKey(String apiKey);
}
