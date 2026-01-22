package com.chartdb.controller;

import com.chartdb.dto.response.ApiResponse;
import com.chartdb.model.UserAIConfig;
import com.chartdb.repository.UserAIConfigRepository;
import com.chartdb.repository.UserRepository;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.ai.AIProvider;
import com.chartdb.service.ai.AIProviderFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AIConfigController {
    
    private final UserAIConfigRepository aiConfigRepository;
    private final UserRepository userRepository;
    private final AIProviderFactory providerFactory;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig(
            @CurrentUser UserPrincipal currentUser) {
        String userId = currentUser.getId();
        
        UserAIConfig config = aiConfigRepository.findByUserId(userId).orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        if (config != null) {
            Map<String, Object> configData = new HashMap<>(config.getConfig());
            // Don't expose full API key
            if (configData.containsKey("apiKey")) {
                String apiKey = (String) configData.get("apiKey");
                configData.put("apiKey", maskApiKey(apiKey));
            }
            response.put("config", configData);
            response.put("usageStats", config.getUsageStats());
            response.put("configured", true);
        } else {
            response.put("configured", false);
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveConfig(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody Map<String, Object> configRequest) {
        
        String userId = currentUser.getId();
        String provider = (String) configRequest.get("provider");
        String apiKey = (String) configRequest.get("apiKey");
        String model = (String) configRequest.get("model");
        
        if (provider == null || apiKey == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Provider and API key are required"));
        }
        
        // Validate provider
        try {
            AIProvider.fromCode(provider);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid provider: " + provider));
        }
        
        // Validate API key format
        if (!providerFactory.getProvider(provider).validateApiKey(apiKey)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid API key format for " + provider));
        }
        
        // Save configuration
        UserAIConfig config = aiConfigRepository.findByUserId(userId)
            .orElse(UserAIConfig.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .build());
        
        Map<String, Object> configData = new HashMap<>();
        configData.put("provider", provider);
        configData.put("apiKey", apiKey);
        if (model != null) {
            configData.put("model", model);
        }
        
        config.setConfig(configData);
        
        if (config.getUsageStats() == null) {
            config.setUsageStats(new HashMap<>());
        }
        
        config = aiConfigRepository.save(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "AI configuration saved successfully");
        response.put("provider", provider);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviders() {
        Map<String, Object> response = new HashMap<>();
        
        var providers = Arrays.stream(AIProvider.values())
            .map(p -> Map.of(
                "code", p.getCode(),
                "name", p.getDisplayName()
            ))
            .toList();
        
        response.put("providers", providers);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteConfig(
            @CurrentUser UserPrincipal currentUser) {
        String userId = currentUser.getId();
        
        aiConfigRepository.findByUserId(userId).ifPresent(aiConfigRepository::delete);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
