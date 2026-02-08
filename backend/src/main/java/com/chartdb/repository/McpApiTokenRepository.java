package com.chartdb.repository;

import com.chartdb.model.McpApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface McpApiTokenRepository extends JpaRepository<McpApiToken, String> {
    
    Optional<McpApiToken> findByTokenHash(String tokenHash);
    
    List<McpApiToken> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);
    
    List<McpApiToken> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<McpApiToken> findByIdAndUserId(String id, String userId);
    
    long countByUserIdAndIsActiveTrue(String userId);
    
    @Modifying
    @Query("UPDATE McpApiToken t SET t.lastUsedAt = :lastUsedAt WHERE t.id = :id")
    void updateLastUsedAt(@Param("id") String id, @Param("lastUsedAt") Instant lastUsedAt);
    
    @Modifying
    @Query("UPDATE McpApiToken t SET t.isActive = false WHERE t.user.id = :userId")
    void revokeAllByUserId(@Param("userId") String userId);
}
