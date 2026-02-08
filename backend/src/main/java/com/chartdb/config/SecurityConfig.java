package com.chartdb.config;

import com.chartdb.security.CustomUserDetailsService;
import com.chartdb.security.JwtAuthenticationEntryPoint;
import com.chartdb.security.JwtAuthenticationFilter;
import com.chartdb.security.McpApiTokenAuthFilter;
import com.chartdb.security.oauth2.CustomOAuth2UserService;
import com.chartdb.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.chartdb.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final McpApiTokenAuthFilter mcpApiTokenAuthFilter;
    private final OAuth2Config oAuth2Config;
    
    // Lazy load OAuth2 components - they're only needed when OAuth2 is enabled
    @Autowired(required = false)
    @Lazy
    private CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired(required = false)
    @Lazy
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    
    @Autowired(required = false)
    @Lazy
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;
    
    @Value("${security.bcrypt.strength:10}")
    private int bcryptStrength;
    
    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            McpApiTokenAuthFilter mcpApiTokenAuthFilter,
            OAuth2Config oAuth2Config) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.mcpApiTokenAuthFilter = mcpApiTokenAuthFilter;
        this.oAuth2Config = oAuth2Config;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Configurable BCrypt strength per environment
        return new BCryptPasswordEncoder(bcryptStrength);
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // Well-known discovery endpoints (prevents 401 on OAuth/MCP metadata discovery)
                .requestMatchers("/.well-known/**").permitAll()
                // OAuth standard endpoints - return 404 so MCP clients don't trigger OAuth flow
                .requestMatchers("/authorize", "/token", "/register").permitAll()
                // MCP endpoints (JSON-RPC handles auth internally per method)
                .requestMatchers("/api/mcp", "/api/mcp/**").permitAll()
                // Swagger/OpenAPI
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        
        // Only configure OAuth2 login if at least one provider is configured
        if (oAuth2Config.isOAuth2Enabled()) {
            log.info("OAuth2 providers configured - enabling OAuth2 login");
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorize"))
                .redirectionEndpoint(redirect -> redirect.baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        } else {
            log.info("No OAuth2 providers configured - OAuth2 login disabled");
        }
        
        http.authenticationProvider(authenticationProvider());
        // MCP API token filter runs first — if token starts with "mcp_", it authenticates via API token
        // Otherwise, falls through to JWT authentication
        http.addFilterBefore(mcpApiTokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(jwtAuthenticationFilter, McpApiTokenAuthFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns instead of allowedOrigins for better flexibility with credentials
        // Allow all origins patterns for flexibility - restrict in production via environment variable
        List<String> patterns = new ArrayList<>(Arrays.asList(
            "http://localhost:*", 
            "http://127.0.0.1:*", 
            "http://192.168.*.*:*",
            "http://62.84.186.161:*",
            "https://62.84.186.161:*",
            "https://chartdb.in",
            "https://www.chartdb.in",
            "https://*.chartdb.in"
        ));
        // Add configured origins
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                if (!patterns.contains(origin)) {
                    patterns.add(origin);
                }
            }
        }
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
