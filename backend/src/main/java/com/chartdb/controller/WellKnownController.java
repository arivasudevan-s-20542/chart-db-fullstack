package com.chartdb.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles OAuth discovery and standard OAuth endpoints.
 * Returns proper RFC 9728 Protected Resource Metadata to indicate this server
 * uses pre-provisioned Bearer API tokens — NOT OAuth.
 * 
 * Without these handlers, unauthenticated requests hit Spring Security's
 * "anyRequest().authenticated()" rule and return 401, which MCP clients
 * interpret as "OAuth is available" — triggering unwanted OAuth flows.
 */
@RestController
public class WellKnownController {

    // ==========================================
    // .well-known discovery endpoints
    // ==========================================

    /**
     * OAuth Authorization Server Metadata (RFC 8414).
     * Returns 404 — no authorization server exists.
     */
    @RequestMapping(value = "/.well-known/oauth-authorization-server", method = RequestMethod.GET)
    public ResponseEntity<Void> oauthAuthorizationServer() {
        return ResponseEntity.notFound().build();
    }

    /**
     * OAuth Protected Resource Metadata (RFC 9728).
     * Returns metadata indicating Bearer tokens are accepted but NO authorization server exists.
     * Without "authorization_servers", clients should not attempt OAuth.
     */
    @RequestMapping(value = "/.well-known/oauth-protected-resource", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Map<String, Object>> oauthProtectedResource(
            jakarta.servlet.http.HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return ResponseEntity.ok(Map.of(
            "resource", baseUrl + "/api/mcp",
            "bearer_methods_supported", List.of("header"),
            "resource_documentation", "https://chartdb.in/docs/mcp"
        ));
    }

    @RequestMapping(value = "/.well-known/openid-configuration", method = RequestMethod.GET)
    public ResponseEntity<Void> openidConfiguration() {
        return ResponseEntity.notFound().build();
    }

    // ==========================================
    // OAuth standard endpoints at server root
    // VS Code MCP client probes these per RFC 8414
    // ==========================================

    @RequestMapping(value = "/authorize", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> authorize() {
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/token", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> token() {
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/register", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> register() {
        return ResponseEntity.notFound().build();
    }
}
