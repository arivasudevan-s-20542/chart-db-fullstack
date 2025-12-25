package com.chartdb.controller;

import com.chartdb.config.OAuth2Config;
import com.chartdb.dto.request.LoginRequest;
import com.chartdb.dto.request.RefreshTokenRequest;
import com.chartdb.dto.request.RegisterRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.AuthResponse;
import com.chartdb.dto.response.UserResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final OAuth2Config oAuth2Config;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful", response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserResponse response = authService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentUser UserPrincipal currentUser) {
        // Client-side logout (invalidate token on client)
        // In production, you might add the token to a blacklist
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
    
    /**
     * Get OAuth2 provider availability status
     * This endpoint is public and returns which OAuth2 providers are configured
     */
    @GetMapping("/oauth2/providers")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getOAuth2Providers() {
        Map<String, Boolean> providers = oAuth2Config.getAvailableProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }
}
