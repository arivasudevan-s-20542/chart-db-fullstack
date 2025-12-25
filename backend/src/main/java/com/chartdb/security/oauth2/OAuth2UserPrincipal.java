package com.chartdb.security.oauth2;

import com.chartdb.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * OAuth2User implementation that wraps our User entity
 */
@Getter
public class OAuth2UserPrincipal implements OAuth2User {
    
    private final String id;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final String cursorColor;
    private final Map<String, Object> attributes;
    
    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getEffectiveDisplayName();
        this.avatarUrl = user.getAvatarUrl();
        this.cursorColor = user.getCursorColor();
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getName() {
        return id;
    }
}
