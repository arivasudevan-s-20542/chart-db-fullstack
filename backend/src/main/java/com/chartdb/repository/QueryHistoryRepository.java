package com.chartdb.repository;

import com.chartdb.model.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, String> {
    
    Page<QueryHistory> findByUserIdOrderByExecutedAtDesc(String userId, Pageable pageable);
    
    Page<QueryHistory> findByConnectionIdOrderByExecutedAtDesc(String connectionId, Pageable pageable);
    
    List<QueryHistory> findTop10ByUserIdOrderByExecutedAtDesc(String userId);
    
    long countByUserIdAndExecutedAtAfter(String userId, Instant after);
}
