package com.chartdb.dto.websocket;

import com.chartdb.dto.response.CollaboratorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceMessage {
    
    private String type; // JOIN, LEAVE, UPDATE, IDLE
    private String diagramId;
    private String action; // joined, left
    private CollaboratorResponse collaborator;
    
    // Denormalized user info for quick access
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;
    
    // Timestamp - can be Long (millis) or Instant
    private Long timestamp;
    private Instant timestampInstant;
    
    // Builder helper for Instant timestamp
    public static class PresenceMessageBuilder {
        public PresenceMessageBuilder timestamp(Long millis) {
            this.timestamp = millis;
            return this;
        }
        public PresenceMessageBuilder timestamp(Instant instant) {
            this.timestampInstant = instant;
            this.timestamp = instant != null ? instant.toEpochMilli() : null;
            return this;
        }
    }
}
