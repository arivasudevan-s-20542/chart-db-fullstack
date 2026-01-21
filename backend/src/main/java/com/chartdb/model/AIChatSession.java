package com.chartdb.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "ai_chat_sessions", indexes = {
    @Index(name = "idx_ai_chat_sessions_diagram", columnList = "diagram_id"),
    @Index(name = "idx_ai_chat_sessions_user", columnList = "user_id"),
    @Index(name = "idx_ai_chat_sessions_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AIChatSession extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Type(JsonBinaryType.class)
    @Column(name = "agent_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> agentConfig;
    
    @Type(JsonBinaryType.class)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;
    
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();
    
    @Column(name = "last_message_at", nullable = false)
    @Builder.Default
    private Instant lastMessageAt = Instant.now();
    
    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
