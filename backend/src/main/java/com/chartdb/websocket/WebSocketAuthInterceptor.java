package com.chartdb.websocket;

import com.chartdb.security.CustomUserDetailsService;
import com.chartdb.security.JwtProvider;
import com.chartdb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            
            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                String userId = jwtProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                accessor.setUser(authentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("WebSocket authenticated for user: {}", userId);
            }
        }
        
        return message;
    }
    
    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        
        // Try token header directly
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        
        return null;
    }
}
