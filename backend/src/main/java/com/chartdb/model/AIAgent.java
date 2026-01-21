package com.chartdb.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "ai_agents", indexes = {
    @Index(name = "idx_ai_agents_user", columnList = "user_id"),
    @Index(name = "idx_ai_agents_public", columnList = "is_public"),
    @Index(name = "idx_ai_agents_system", columnList = "is_system")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AIAgent extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // NULL for system agents
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Type(JsonBinaryType.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;
    
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;
}
