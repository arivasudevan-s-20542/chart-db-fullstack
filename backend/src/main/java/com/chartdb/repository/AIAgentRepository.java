package com.chartdb.repository;

import com.chartdb.model.AIAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIAgentRepository extends JpaRepository<AIAgent, String> {
    
    List<AIAgent> findByIsSystemTrueOrderByNameAsc();
    
    List<AIAgent> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<AIAgent> findByIsPublicTrueOrderByUsageCountDesc();
    
    List<AIAgent> findByUserIdOrIsSystemTrueOrderByNameAsc(String userId);
}
