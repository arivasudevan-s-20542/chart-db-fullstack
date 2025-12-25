package com.chartdb.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds user information extracted from OAuth2 provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo {
    private String id;
    private String name;
    private String email;
    private String imageUrl;
    private OAuth2Provider provider;
    
    /**
     * Extract user info from Google OAuth2 response
     */
    public static OAuth2UserInfo fromGoogle(java.util.Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
            .id((String) attributes.get("sub"))
            .name((String) attributes.get("name"))
            .email((String) attributes.get("email"))
            .imageUrl((String) attributes.get("picture"))
            .provider(OAuth2Provider.GOOGLE)
            .build();
    }
    
    /**
     * Extract user info from GitHub OAuth2 response
     */
    public static OAuth2UserInfo fromGitHub(java.util.Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
            .id(String.valueOf(attributes.get("id")))
            .name((String) attributes.getOrDefault("name", attributes.get("login")))
            .email((String) attributes.get("email"))
            .imageUrl((String) attributes.get("avatar_url"))
            .provider(OAuth2Provider.GITHUB)
            .build();
    }
    
    /**
     * Extract user info from Zoho OAuth2 response
     */
    public static OAuth2UserInfo fromZoho(java.util.Map<String, Object> attributes) {
        // Zoho returns user info in a nested structure
        String displayName = (String) attributes.getOrDefault("Display_Name", 
            attributes.getOrDefault("display_name", attributes.get("name")));
        String email = (String) attributes.getOrDefault("Email", 
            attributes.getOrDefault("email", null));
        String id = (String) attributes.getOrDefault("ZUID", 
            String.valueOf(attributes.getOrDefault("zuid", attributes.get("id"))));
            
        return OAuth2UserInfo.builder()
            .id(id)
            .name(displayName)
            .email(email)
            .imageUrl(null) // Zoho doesn't provide avatar URL directly
            .provider(OAuth2Provider.ZOHO)
            .build();
    }
    
    /**
     * Factory method to extract user info based on provider
     */
    public static OAuth2UserInfo fromOAuth2(String registrationId, java.util.Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> fromGoogle(attributes);
            case "github" -> fromGitHub(attributes);
            case "zoho" -> fromZoho(attributes);
            default -> throw new IllegalArgumentException("Unknown OAuth2 provider: " + registrationId);
        };
    }
}
