package com.chartdb.security.oauth2;

import com.chartdb.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens
 * and redirecting to the frontend with tokens in URL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtProvider jwtProvider;
    
    @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String redirectUri;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        
        String accessToken = jwtProvider.generateAccessToken(principal.getId(), principal.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(principal.getId());
        
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("access_token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
            .queryParam("refresh_token", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
            .build().toUriString();
        
        log.info("OAuth2 authentication successful for user: {}, redirecting to: {}", 
            principal.getEmail(), redirectUri);
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }
        
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
