package com.chartdb.repository;

import com.chartdb.model.SavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, String> {
    
    List<SavedQuery> findByDiagramIdOrderByCreatedAtDesc(String diagramId);
    
    List<SavedQuery> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SavedQuery> findByIsPublicTrueOrderByUsageCountDesc();
    
    Optional<SavedQuery> findByIdAndUserId(String id, String userId);
}
