package com.chartdb.controller;

import com.chartdb.dto.request.ChatMessageRequest;
import com.chartdb.dto.request.StartChatSessionRequest;
import com.chartdb.dto.response.AIChatSessionResponse;
import com.chartdb.dto.response.AIMessageResponse;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.AIAssistantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AIAssistantController {
    
    private final AIAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<AIChatSessionResponse>> startChatSession(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody StartChatSessionRequest request) {
        AIChatSessionResponse response = aiAssistantService.startChatSession(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Chat session started", response));
    }
    
    @GetMapping("/sessions/diagram/{diagramId}")
    public ResponseEntity<ApiResponse<List<AIChatSessionResponse>>> getActiveSessions(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<AIChatSessionResponse> sessions = aiAssistantService.getActiveSessions(currentUser.getId(), diagramId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request) {
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout
        
        // Handle timeout and completion
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout for session: {}", sessionId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("SSE emitter error for session: {}", sessionId, ex);
            emitter.completeWithError(ex);
        });
        
        // Run streaming in managed thread pool
        executorService.execute(() -> {
            try {
                log.debug("Starting SSE stream for session: {}", sessionId);
                
                AIMessageResponse response = aiAssistantService.sendMessageStreaming(
                    sessionId, 
                    currentUser.getId(), 
                    request,
                    (chunk) -> {
                        try {
                            // Send each chunk as SSE event
                            emitter.send(SseEmitter.event()
                                .data("{\"content\":\"" + escapeJson(chunk) + "\"}")
                                .name("message"));
                        } catch (IOException e) {
                            log.error("Failed to send SSE chunk", e);
                            emitter.completeWithError(e);
                        }
                    }
                );
                
                log.debug("Completed AI response, sending final event");
                
                // Send final completion event with full message
                emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(ApiResponse.success(response)))
                    .name("done"));
                
                emitter.complete();
                log.debug("SSE stream completed for session: {}", sessionId);
                
            } catch (Exception e) {
                log.error("Error in SSE stream for session: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}")
                        .name("error"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<ApiResponse<List<AIMessageResponse>>> getChatHistory(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String sessionId) {
        List<AIMessageResponse> history = aiAssistantService.getChatHistory(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> endSession(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String sessionId) {
        aiAssistantService.endSession(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Chat session ended", null));
    }
}
