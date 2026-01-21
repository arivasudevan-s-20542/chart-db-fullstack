package com.chartdb.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIProviderFactory {
    
    private final List<AIProviderService> providers;
    private Map<AIProvider, AIProviderService> providerMap;
    
    /**
     * Get the appropriate AI provider service
     */
    public AIProviderService getProvider(AIProvider provider) {
        if (providerMap == null) {
            providerMap = providers.stream()
                .collect(Collectors.toMap(AIProviderService::getProvider, Function.identity()));
        }
        
        AIProviderService service = providerMap.get(provider);
        if (service == null) {
            throw new IllegalArgumentException("No service found for provider: " + provider);
        }
        return service;
    }
    
    /**
     * Get provider service by code
     */
    public AIProviderService getProvider(String providerCode) {
        AIProvider provider = AIProvider.fromCode(providerCode);
        return getProvider(provider);
    }
}
