package com.chartdb.repository;

import com.chartdb.model.AISuggestedChange;
import com.chartdb.model.enums.ChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AISuggestedChangeRepository extends JpaRepository<AISuggestedChange, String> {
    
    List<AISuggestedChange> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    List<AISuggestedChange> findBySessionIdAndStatus(String sessionId, ChangeStatus status);
    
    long countBySessionIdAndStatus(String sessionId, ChangeStatus status);
}
