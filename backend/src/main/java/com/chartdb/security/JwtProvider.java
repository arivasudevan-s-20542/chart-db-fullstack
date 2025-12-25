package com.chartdb.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    private SecretKey key;
    
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal.getId(), userPrincipal.getEmail());
    }
    
    public String generateAccessToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }
    
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        
        return Jwts.builder()
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }
    
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("email", String.class);
    }
    
    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("type", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
    
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }
    
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }
    
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
