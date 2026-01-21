package com.chartdb.repository;

import com.chartdb.model.MCPServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MCPServerRepository extends JpaRepository<MCPServer, String> {
    
    List<MCPServer> findByUserIdAndIsActiveTrue(String userId);
    
    List<MCPServer> findByUserIdOrderByCreatedAtDesc(String userId);
}
