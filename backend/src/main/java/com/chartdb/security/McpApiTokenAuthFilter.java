package com.chartdb.security;

import com.chartdb.model.McpApiToken;
import com.chartdb.service.McpApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Security filter that authenticates requests using MCP API tokens.
 * This runs BEFORE the JWT filter. If a Bearer token starts with "mcp_",
 * it's treated as an API token. Otherwise, it falls through to JWT auth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpApiTokenAuthFilter extends OncePerRequestFilter {
    
    private final McpApiTokenService mcpApiTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractBearerToken(request);
            
            // Only process if it looks like an MCP API token
            if (token != null && McpApiTokenService.isMcpApiToken(token)) {
                Optional<McpApiToken> apiTokenOpt = mcpApiTokenService.validateToken(token);
                
                if (apiTokenOpt.isPresent()) {
                    McpApiToken apiToken = apiTokenOpt.get();
                    String userId = apiToken.getUser().getId();
                    
                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                    
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Update last used timestamp asynchronously
                    mcpApiTokenService.updateLastUsed(apiToken.getId());
                    
                    log.debug("Authenticated via MCP API token '{}' for user {}",
                        apiToken.getName(), userId);
                } else {
                    log.debug("Invalid or expired MCP API token presented");
                }
            }
        } catch (Exception ex) {
            log.error("Could not authenticate with MCP API token", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
