package com.chartdb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        log.error("Responding with unauthorized error. Method={} URI={} QueryString={} Message={}",
            request.getMethod(), request.getRequestURI(), request.getQueryString(), authException.getMessage());
        
        // For MCP-related paths, return 404 instead of 401 to prevent VS Code
        // from triggering OAuth flows. VS Code interprets any 401 as "needs OAuth".
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/mcp") || uri.equals("/authorize") || uri.equals("/token") || uri.equals("/register"))) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"not_found\"}");
            return;
        }
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\": false, \"message\": \"Unauthorized - " + authException.getMessage() + "\"}");
    }
}
