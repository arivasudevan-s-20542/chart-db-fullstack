package com.chartdb.controller;

import com.chartdb.dto.request.CreateMcpApiTokenRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.McpApiTokenResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.McpApiTokenService;
import com.chartdb.service.McpApiTokenService.McpApiTokenCreateResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing MCP API tokens.
 * Tokens allow external MCP clients (like Claude Desktop) to authenticate
 * without going through the browser-based JWT login flow.
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/tokens")
@RequiredArgsConstructor
public class McpApiTokenController {
    
    private final McpApiTokenService tokenService;
    
    /**
     * Create a new MCP API token.
     * The plain-text token is only returned once in this response.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<McpApiTokenResponse>> createToken(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateMcpApiTokenRequest request) {
        
        McpApiTokenCreateResult result = tokenService.createToken(
            currentUser.getId(),
            request.getName(),
            request.getScopes(),
            request.getExpiresAt()
        );
        
        McpApiTokenResponse response = McpApiTokenResponse.fromEntityWithToken(
            result.token(), result.plainTextToken()
        );
        
        return ResponseEntity.ok(ApiResponse.success("API token created successfully", response));
    }
    
    /**
     * List all MCP API tokens for the current user.
     * Plain-text tokens are NOT returned — only metadata.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<McpApiTokenResponse>>> listTokens(
            @CurrentUser UserPrincipal currentUser) {
        
        List<McpApiTokenResponse> tokens = tokenService.listTokens(currentUser.getId())
            .stream()
            .map(McpApiTokenResponse::fromEntity)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }
    
    /**
     * Revoke (deactivate) a specific MCP API token.
     */
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> revokeToken(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tokenId) {
        
        boolean revoked = tokenService.revokeToken(tokenId, currentUser.getId());
        
        if (revoked) {
            return ResponseEntity.ok(ApiResponse.success("Token revoked successfully", Map.of("revoked", true)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Revoke all MCP API tokens for the current user.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> revokeAllTokens(
            @CurrentUser UserPrincipal currentUser) {
        
        tokenService.revokeAllTokens(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("All tokens revoked", Map.of("revoked", true)));
    }
}
