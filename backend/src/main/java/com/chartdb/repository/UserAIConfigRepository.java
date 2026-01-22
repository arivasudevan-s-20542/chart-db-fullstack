package com.chartdb.repository;

import com.chartdb.model.UserAIConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAIConfigRepository extends JpaRepository<UserAIConfig, String> {
    
    Optional<UserAIConfig> findByUserId(String userId);
}
