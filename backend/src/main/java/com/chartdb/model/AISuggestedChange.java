package com.chartdb.model;

import com.chartdb.model.enums.ChangeStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "ai_suggested_changes", indexes = {
    @Index(name = "idx_ai_suggested_changes_session", columnList = "session_id"),
    @Index(name = "idx_ai_suggested_changes_pending", columnList = "session_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AISuggestedChange extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AIChatSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private AIMessage message;
    
    @Type(JsonBinaryType.class)
    @Column(name = "changes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> changes;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ChangeStatus status = ChangeStatus.PENDING;
    
    @Type(JsonBinaryType.class)
    @Column(name = "applied_changes", columnDefinition = "jsonb")
    private Map<String, Object> appliedChanges;
    
    @Column(name = "applied_at")
    private Instant appliedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private User appliedBy;
}
