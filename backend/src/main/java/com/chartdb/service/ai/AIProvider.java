package com.chartdb.service.ai;

/**
 * AI provider types supported by the system
 */
public enum AIProvider {
    OPENAI("openai", "OpenAI"),
    GEMINI("gemini", "Google Gemini"),
    CLAUDE("claude", "Anthropic Claude"),
    MISTRAL("mistral", "Mistral AI"),
    DEEPSEEK("deepseek", "DeepSeek");
    
    private final String code;
    private final String displayName;
    
    AIProvider(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static AIProvider fromCode(String code) {
        for (AIProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown AI provider: " + code);
    }
}
