package com.chartdb.repository;

import com.chartdb.model.AIChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIChatSessionRepository extends JpaRepository<AIChatSession, String> {
    
    List<AIChatSession> findByDiagramIdOrderByLastMessageAtDesc(String diagramId);
    
    List<AIChatSession> findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId);
    
    Optional<AIChatSession> findByIdAndUserId(String id, String userId);
    
    List<AIChatSession> findTop10ByDiagramIdAndIsActiveTrueOrderByLastMessageAtDesc(String diagramId);
}
