package com.chartdb.security.oauth2;

import lombok.Getter;

/**
 * Enum for supported OAuth2 providers
 */
@Getter
public enum OAuth2Provider {
    GOOGLE("google"),
    GITHUB("github"),
    ZOHO("zoho");
    
    private final String name;
    
    OAuth2Provider(String name) {
        this.name = name;
    }
    
    public static OAuth2Provider fromString(String name) {
        for (OAuth2Provider provider : OAuth2Provider.values()) {
            if (provider.name.equalsIgnoreCase(name)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth2 provider: " + name);
    }
}
