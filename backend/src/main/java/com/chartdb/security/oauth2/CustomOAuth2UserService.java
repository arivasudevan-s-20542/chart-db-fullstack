package com.chartdb.security.oauth2;

import com.chartdb.model.User;
import com.chartdb.repository.UserRepository;
import com.chartdb.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

/**
 * Custom OAuth2 user service that processes OAuth2 authentication
 * and creates/updates users in the database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    
    private static final String[] CURSOR_COLORS = {
        "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16",
        "#22C55E", "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9",
        "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
        "#EC4899", "#F43F5E"
    };
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 authentication: " + ex.getMessage());
        }
    }
    
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfo.fromOAuth2(registrationId, oauth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            // GitHub might not return email if it's private
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider. Please make your email public or use another login method.");
        }
        
        // First try to find user by OAuth provider and ID
        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthProviderId(
            userInfo.getProvider().getName(), userInfo.getId()
        );
        
        User user;
        if (existingUser.isPresent()) {
            // User exists with this OAuth account - update and return
            user = updateExistingOAuthUser(existingUser.get(), userInfo);
            log.info("OAuth2 user logged in: {} via {}", user.getEmail(), userInfo.getProvider());
        } else {
            // Check if user exists with this email (might have registered with email/password)
            Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(userInfo.getEmail());
            if (userByEmail.isPresent()) {
                // Link OAuth account to existing user
                user = linkOAuthToExistingUser(userByEmail.get(), userInfo);
                log.info("Linked OAuth2 account to existing user: {} via {}", user.getEmail(), userInfo.getProvider());
            } else {
                // Create new user
                user = createNewOAuthUser(userInfo);
                log.info("New OAuth2 user registered: {} via {}", user.getEmail(), userInfo.getProvider());
            }
        }
        
        // Update last login
        userRepository.updateLastLogin(user.getId(), Instant.now());
        
        // Return custom OAuth2User with our user ID attached
        return new OAuth2UserPrincipal(user, oauth2User.getAttributes());
    }
    
    private User updateExistingOAuthUser(User user, OAuth2UserInfo userInfo) {
        // Update avatar if available
        if (userInfo.getImageUrl() != null && !userInfo.getImageUrl().isBlank()) {
            user.setAvatarUrl(userInfo.getImageUrl());
        }
        // Update display name if changed
        if (userInfo.getName() != null && !userInfo.getName().isBlank()) {
            user.setDisplayName(userInfo.getName());
        }
        return userRepository.save(user);
    }
    
    private User linkOAuthToExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setOauthProvider(userInfo.getProvider().getName());
        user.setOauthProviderId(userInfo.getId());
        // Update avatar if not set
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && userInfo.getImageUrl() != null) {
            user.setAvatarUrl(userInfo.getImageUrl());
        }
        return userRepository.save(user);
    }
    
    private User createNewOAuthUser(OAuth2UserInfo userInfo) {
        String username = generateUsernameFromEmail(userInfo.getEmail());
        String cursorColor = CURSOR_COLORS[new Random().nextInt(CURSOR_COLORS.length)];
        
        User user = User.builder()
            .email(userInfo.getEmail())
            .username(username)
            .displayName(userInfo.getName())
            .avatarUrl(userInfo.getImageUrl())
            .oauthProvider(userInfo.getProvider().getName())
            .oauthProviderId(userInfo.getId())
            .cursorColor(cursorColor)
            .isActive(true)
            .isVerified(true) // OAuth users are verified by the provider
            .build();
        
        return userRepository.save(user);
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
}
