package com.chartdb.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom OAuth2 configuration that only registers providers with valid credentials.
 * This prevents startup failures when OAuth2 credentials are not configured.
 */
@Configuration
public class OAuth2Config {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Config.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.github.client-id:}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret:}")
    private String githubClientSecret;

    @Value("${spring.security.oauth2.client.registration.zoho.client-id:}")
    private String zohoClientId;

    @Value("${spring.security.oauth2.client.registration.zoho.client-secret:}")
    private String zohoClientSecret;

    private List<ClientRegistration> registrations;
    private boolean oauth2Enabled = false;

    @PostConstruct
    public void init() {
        registrations = new ArrayList<>();

        // Only add Google if credentials are configured
        if (isNotEmpty(googleClientId) && isNotEmpty(googleClientSecret)) {
            registrations.add(googleClientRegistration());
            logger.info("Google OAuth2 provider configured");
        }

        // Only add GitHub if credentials are configured
        if (isNotEmpty(githubClientId) && isNotEmpty(githubClientSecret)) {
            registrations.add(githubClientRegistration());
            logger.info("GitHub OAuth2 provider configured");
        }

        // Only add Zoho if credentials are configured
        if (isNotEmpty(zohoClientId) && isNotEmpty(zohoClientSecret)) {
            registrations.add(zohoClientRegistration());
            logger.info("Zoho OAuth2 provider configured");
        }

        oauth2Enabled = !registrations.isEmpty();
        
        if (oauth2Enabled) {
            logger.info("OAuth2 authentication enabled with {} provider(s)", registrations.size());
        } else {
            logger.info("OAuth2 authentication disabled - no providers configured");
        }
    }

    /**
     * Create ClientRegistrationRepository only if OAuth2 is enabled.
     * Returns null when no providers are configured to prevent bean creation failure.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Re-check registrations since PostConstruct may not have run yet
        if (registrations == null) {
            init();
        }
        
        if (registrations.isEmpty()) {
            // Return null to signal that no OAuth2 config is available
            // SecurityConfig will check isOAuth2Enabled() before using OAuth2
            logger.debug("No OAuth2 providers configured, skipping ClientRegistrationRepository bean creation");
            return null;
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * Check if OAuth2 authentication is enabled (at least one provider configured).
     */
    public boolean isOAuth2Enabled() {
        if (registrations == null) {
            init();
        }
        return oauth2Enabled;
    }

    /**
     * Get available OAuth2 providers.
     */
    public Map<String, Boolean> getAvailableProviders() {
        Map<String, Boolean> providers = new HashMap<>();
        providers.put("google", isNotEmpty(googleClientId) && isNotEmpty(googleClientSecret));
        providers.put("github", isNotEmpty(githubClientId) && isNotEmpty(githubClientSecret));
        providers.put("zoho", isNotEmpty(zohoClientId) && isNotEmpty(zohoClientSecret));
        return providers;
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId(googleClientId)
            .clientSecret(googleClientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build();
    }

    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
            .clientId(githubClientId)
            .clientSecret(githubClientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("user:email", "read:user")
            .authorizationUri("https://github.com/login/oauth2/authorize")
            .tokenUri("https://github.com/login/oauth2/access_token")
            .userInfoUri("https://api.github.com/user")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    }

    private ClientRegistration zohoClientRegistration() {
        return ClientRegistration.withRegistrationId("zoho")
            .clientId(zohoClientId)
            .clientSecret(zohoClientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("AaaServer.profile.READ")
            .authorizationUri("https://accounts.zoho.com/oauth/v2/auth")
            .tokenUri("https://accounts.zoho.com/oauth/v2/token")
            .userInfoUri("https://accounts.zoho.com/oauth/user/info")
            .userNameAttributeName("Email")
            .clientName("Zoho")
            .build();
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Check if any OAuth2 provider is configured
     */
    public boolean isOAuth2Enabled() {
        return (isNotEmpty(googleClientId) && isNotEmpty(googleClientSecret)) ||
               (isNotEmpty(githubClientId) && isNotEmpty(githubClientSecret)) ||
               (isNotEmpty(zohoClientId) && isNotEmpty(zohoClientSecret));
    }
    
    // /**
    //  * Get map of available OAuth2 providers
    //  */
    // public java.util.Map<String, Boolean> getAvailableProviders() {
    //     return java.util.Map.of(
    //         "google", isNotEmpty(googleClientId) && isNotEmpty(googleClientSecret),
    //         "github", isNotEmpty(githubClientId) && isNotEmpty(githubClientSecret),
    //         "zoho", isNotEmpty(zohoClientId) && isNotEmpty(zohoClientSecret)
    //     );
    // }
}
