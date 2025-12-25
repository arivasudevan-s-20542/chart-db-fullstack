# 13 - Security & JWT Implementation

## 🔐 JWT Token Provider

```java
package com.chartdb.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration:3600000}") // 1 hour
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration:604800000}") // 7 days
    private long refreshTokenExpiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Generate access token from authentication
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }
    
    /**
     * Generate access token from user principal
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getId().toString())
            .claim("email", userPrincipal.getEmail())
            .claim("displayName", userPrincipal.getDisplayName())
            .claim("type", "access")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Generate refresh token
     */
    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getId().toString())
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .setId(UUID.randomUUID().toString()) // Unique token ID for revocation
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Get user ID from JWT token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * Get user principal from JWT token
     */
    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return UserPrincipal.builder()
            .id(UUID.fromString(claims.getSubject()))
            .email(claims.get("email", String.class))
            .displayName(claims.get("displayName", String.class))
            .build();
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
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
    
    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get token expiration time in seconds
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }
}
```

---

## 👤 User Principal

```java
package com.chartdb.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Data
@Builder
public class UserPrincipal implements UserDetails {
    
    private UUID id;
    private String email;
    private String displayName;
    private String password;
    private String avatarUrl;
    private boolean enabled;
    private Collection<? extends GrantedAuthority> authorities;
    
    public static UserPrincipal create(com.chartdb.entity.User user) {
        return UserPrincipal.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .password(user.getPasswordHash())
            .avatarUrl(user.getAvatarUrl())
            .enabled(true)
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
            ))
            .build();
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
```

---

## 🔍 JWT Authentication Filter

```java
package com.chartdb.security;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Don't use refresh tokens for authentication
                if (!tokenProvider.isRefreshToken(jwt)) {
                    var userId = tokenProvider.getUserIdFromToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                    
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
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Also check query parameter (for WebSocket)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") ||
               path.startsWith("/ws") ||
               path.equals("/actuator/health");
    }
}
```

---

## 👥 Custom User Details Service

```java
package com.chartdb.security;

import com.chartdb.entity.User;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with email: " + email
            ));
        
        return UserPrincipal.create(user);
    }
    
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with id: " + id
            ));
        
        return UserPrincipal.create(user);
    }
}
```

---

## 🚫 JWT Authentication Entry Point

```java
package com.chartdb.security;

import com.chartdb.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        log.error("Unauthorized error: {}", authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpServletResponse.SC_UNAUTHORIZED)
            .error("Unauthorized")
            .message("Authentication required. Please login.")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
```

---

## 🎯 @CurrentUser Annotation

```java
package com.chartdb.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}

// Usage in controllers:
@GetMapping("/me")
public ResponseEntity<UserDTO> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
    // currentUser is automatically injected
    return ResponseEntity.ok(userService.getUser(currentUser.getId()));
}
```

---

## 🔒 Authentication Controller

```java
package com.chartdb.controller;

import com.chartdb.dto.UserDTO;
import com.chartdb.dto.request.UserLoginRequest;
import com.chartdb.dto.request.UserRegistrationRequest;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser UserPrincipal currentUser) {
        authService.logout(currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(authService.getCurrentUser(currentUser.getId()));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request) {
        authService.sendPasswordResetEmail(request.get("email"));
        return ResponseEntity.ok(Map.of(
            "message", "Password reset email sent if account exists"
        ));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request) {
        authService.resetPassword(request.get("token"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
```

---

## ⚙️ Auth Service

```java
package com.chartdb.service;

import com.chartdb.dto.UserDTO;
import com.chartdb.dto.request.UserLoginRequest;
import com.chartdb.dto.request.UserRegistrationRequest;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.entity.User;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.UnauthorizedException;
import com.chartdb.mapper.UserMapper;
import com.chartdb.repository.UserRepository;
import com.chartdb.security.JwtTokenProvider;
import com.chartdb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    
    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        
        // Create new user
        User user = User.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName())
            .provider("local")
            .emailVerified(false)
            .build();
        
        user = userRepository.save(user);
        
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);
        
        log.info("User registered: {}", user.getEmail());
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpirationInSeconds())
            .user(userMapper.toDTO(user))
            .build();
    }
    
    @Transactional
    public AuthResponse login(UserLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().toLowerCase(),
                request.getPassword()
            )
        );
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Update last login
        userRepository.findById(userPrincipal.getId())
            .ifPresent(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            });
        
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);
        
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow();
        
        log.info("User logged in: {}", user.getEmail());
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpirationInSeconds())
            .user(userMapper.toDTO(user))
            .build();
    }
    
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Not a refresh token");
        }
        
        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);
        
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpirationInSeconds())
            .user(userMapper.toDTO(user))
            .build();
    }
    
    public void logout(UUID userId) {
        // For stateless JWT, logout is handled client-side
        // If using token blacklist, add refresh token to blacklist here
        log.info("User logged out: {}", userId);
    }
    
    public UserDTO getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        return userMapper.toDTO(user);
    }
    
    public void sendPasswordResetEmail(String email) {
        // Generate reset token
        // Send email
        // Implementation depends on email service
    }
    
    public void resetPassword(String token, String newPassword) {
        // Validate reset token
        // Update password
    }
}
```

---

## 🔧 Security Properties

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-for-jwt-signing-min-32-chars}
  access-token-expiration: 3600000    # 1 hour
  refresh-token-expiration: 604800000 # 7 days

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email

cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
```

---

## 🛡️ Method-Level Security

```java
// Enable in SecurityConfig:
@EnableMethodSecurity

// Usage in services/controllers:
@PreAuthorize("@diagramSecurity.isOwner(#diagramId, authentication)")
public void deleteDiagram(UUID diagramId) {
    // Only owner can delete
}

@PreAuthorize("@diagramSecurity.canEdit(#diagramId, authentication)")
public TableDTO createTable(UUID diagramId, CreateTableRequest request) {
    // Owner or editor can create tables
}

// DiagramSecurity component:
@Component("diagramSecurity")
@RequiredArgsConstructor
public class DiagramSecurity {
    
    private final DiagramRepository diagramRepository;
    private final DiagramCollaboratorRepository collaboratorRepository;
    
    public boolean isOwner(UUID diagramId, Authentication auth) {
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        return diagramRepository.findById(diagramId)
            .map(d -> d.getOwner().getId().equals(user.getId()))
            .orElse(false);
    }
    
    public boolean canEdit(UUID diagramId, Authentication auth) {
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        
        // Owner can always edit
        if (isOwner(diagramId, auth)) return true;
        
        // Check collaborator permissions
        return collaboratorRepository
            .findByDiagramIdAndUserId(diagramId, user.getId())
            .map(c -> c.getPermission().equals("edit") || c.getPermission().equals("admin"))
            .orElse(false);
    }
    
    public boolean canView(UUID diagramId, Authentication auth) {
        // Public diagrams can be viewed by anyone
        if (diagramRepository.findById(diagramId)
                .map(Diagram::getIsPublic)
                .orElse(false)) {
            return true;
        }
        
        // Otherwise check ownership or collaboration
        return canEdit(diagramId, auth) || 
               collaboratorRepository.findByDiagramIdAndUserId(diagramId, 
                   ((UserPrincipal) auth.getPrincipal()).getId()).isPresent();
    }
}
```

---

**← Previous:** `12-FRONTEND-INTEGRATION.md` | **Next:** `14-TESTING.md` →
