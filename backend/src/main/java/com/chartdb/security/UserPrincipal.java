package com.chartdb.security;

import com.chartdb.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class UserPrincipal implements UserDetails {
    
    private final String id;
    private final String email;
    private final String password;
    private final String displayName;
    private final String avatarUrl;
    private final String cursorColor;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;
    
    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.avatarUrl = user.getAvatarUrl();
        this.cursorColor = user.getCursorColor();
        this.isActive = user.getIsActive();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
