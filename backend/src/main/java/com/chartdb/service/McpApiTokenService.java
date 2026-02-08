package com.chartdb.service;

import com.chartdb.model.McpApiToken;
import com.chartdb.model.User;
import com.chartdb.repository.McpApiTokenRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpApiTokenService {
    
    private static final String TOKEN_PREFIX = "mcp_";
    private static final int TOKEN_RANDOM_BYTES = 32;
    private static final int MAX_TOKENS_PER_USER = 10;
    
    private final McpApiTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Create a new MCP API token for a user.
     * Returns the plain-text token (only shown once at creation time).
     */
    @Transactional
    public McpApiTokenCreateResult createToken(String userId, String name, String scopes, Instant expiresAt) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        
        // Check token limit
        long activeCount = tokenRepository.countByUserIdAndIsActiveTrue(userId);
        if (activeCount >= MAX_TOKENS_PER_USER) {
            throw new IllegalStateException("Maximum number of API tokens (" + MAX_TOKENS_PER_USER + ") reached. Please revoke an existing token first.");
        }
        
        // Generate secure random token
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Hash the token for storage
        String tokenHash = hashToken(rawToken);
        
        // Extract prefix for identification (first 12 chars)
        String tokenPrefix = rawToken.substring(0, Math.min(12, rawToken.length()));
        
        McpApiToken token = McpApiToken.builder()
            .user(user)
            .name(name)
            .tokenHash(tokenHash)
            .tokenPrefix(tokenPrefix)
            .scopes(scopes != null ? scopes : "mcp:read,mcp:write")
            .expiresAt(expiresAt)
            .isActive(true)
            .build();
        
        McpApiToken saved = tokenRepository.save(token);
        
        log.info("Created MCP API token '{}' for user {}", name, userId);
        
        return new McpApiTokenCreateResult(saved, rawToken);
    }
    
    /**
     * Validate an API token and return the associated token entity.
     */
    @Transactional(readOnly = true)
    public Optional<McpApiToken> validateToken(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        
        String tokenHash = hashToken(rawToken);
        Optional<McpApiToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);
        
        return tokenOpt.filter(McpApiToken::isValid);
    }
    
    /**
     * Update the last used timestamp for a token (async-friendly).
     */
    @Transactional
    public void updateLastUsed(String tokenId) {
        tokenRepository.updateLastUsedAt(tokenId, Instant.now());
    }
    
    /**
     * List all tokens for a user.
     */
    @Transactional(readOnly = true)
    public List<McpApiToken> listTokens(String userId) {
        return tokenRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * List active tokens for a user.
     */
    @Transactional(readOnly = true)
    public List<McpApiToken> listActiveTokens(String userId) {
        return tokenRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Revoke a specific token.
     */
    @Transactional
    public boolean revokeToken(String tokenId, String userId) {
        Optional<McpApiToken> tokenOpt = tokenRepository.findByIdAndUserId(tokenId, userId);
        if (tokenOpt.isPresent()) {
            McpApiToken token = tokenOpt.get();
            token.setIsActive(false);
            tokenRepository.save(token);
            log.info("Revoked MCP API token '{}' for user {}", token.getName(), userId);
            return true;
        }
        return false;
    }
    
    /**
     * Revoke all tokens for a user.
     */
    @Transactional
    public void revokeAllTokens(String userId) {
        tokenRepository.revokeAllByUserId(userId);
        log.info("Revoked all MCP API tokens for user {}", userId);
    }
    
    /**
     * Hash a raw token using SHA-256.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Check if a raw token looks like an MCP API token (starts with prefix).
     */
    public static boolean isMcpApiToken(String token) {
        return token != null && token.startsWith(TOKEN_PREFIX);
    }
    
    /**
     * Result wrapper that includes the plain-text token (only at creation).
     */
    public record McpApiTokenCreateResult(McpApiToken token, String plainTextToken) {}
}
