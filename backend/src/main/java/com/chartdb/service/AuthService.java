package com.chartdb.service;

import com.chartdb.dto.request.LoginRequest;
import com.chartdb.dto.request.RefreshTokenRequest;
import com.chartdb.dto.request.RegisterRequest;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.dto.response.UserResponse;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ConflictException;
import com.chartdb.mapper.UserMapper;
import com.chartdb.model.DiagramPermission;
import com.chartdb.model.User;
import com.chartdb.repository.DiagramPermissionRepository;
import com.chartdb.repository.UserRepository;
import com.chartdb.security.JwtProvider;
import com.chartdb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DiagramPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserMapper userMapper;
    
    private static final String[] CURSOR_COLORS = {
        "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16",
        "#22C55E", "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9",
        "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
        "#EC4899", "#F43F5E"
    };
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        
        // Generate username from email if not provided
        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = generateUsernameFromEmail(request.getEmail());
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken");
        }
        
        String cursorColor = CURSOR_COLORS[new Random().nextInt(CURSOR_COLORS.length)];
        
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName() != null ? request.getDisplayName() : extractNameFromEmail(request.getEmail()))
            .username(username)
            .cursorColor(cursorColor)
            .isActive(true)
            .build();
        
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        
        // Link any pending diagram permissions for this email
        linkPendingPermissions(user);
        
        return createAuthResponse(user);
    }
    
    private String generateUsernameFromEmail(String email) {
        String baseName = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String username = baseName;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseName + suffix++;
        }
        return username;
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userRepository.updateLastLogin(principal.getId(), Instant.now());
        
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new BadRequestException("User not found"));
        
        log.info("User logged in: {}", user.getEmail());
        
        return createAuthResponse(user);
    }
    
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }
        
        String userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User not found"));
        
        if (!user.getIsActive()) {
            throw new BadRequestException("User account is disabled");
        }
        
        return createAuthResponse(user);
    }
    
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User not found"));
        return userMapper.toResponse(user);
    }
    
    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
            .user(userMapper.toResponse(user))
            .build();
    }
    
    private String extractNameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return localPart.substring(0, 1).toUpperCase() + localPart.substring(1).replace(".", " ");
    }
    
    /**
     * Link any pending diagram permissions to the newly registered user.
     * This handles cases where a diagram was shared with an email before the user registered.
     */
    private void linkPendingPermissions(User user) {
        List<DiagramPermission> pendingPermissions = permissionRepository
            .findByInvitedEmailAndInvitationStatus(user.getEmail(), "PENDING");
        
        if (!pendingPermissions.isEmpty()) {
            for (DiagramPermission permission : pendingPermissions) {
                permission.setUser(user);
                permission.setInvitedEmail(null);
                permission.setInvitationStatus("ACCEPTED");
                permission.setAcceptedAt(Instant.now());
            }
            permissionRepository.saveAll(pendingPermissions);
            log.info("Linked {} pending permissions to new user {}", pendingPermissions.size(), user.getEmail());
        }
    }
}
