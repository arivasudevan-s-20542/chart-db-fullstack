package com.chartdb.repository;

import com.chartdb.model.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, String> {
    
    List<AIMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    List<AIMessage> findTop50BySessionIdOrderByCreatedAtDesc(String sessionId);
}
