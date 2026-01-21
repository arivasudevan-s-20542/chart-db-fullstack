package com.chartdb.repository;

import com.chartdb.model.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, String> {
    
    List<DatabaseConnection> findByDiagramIdOrderByCreatedAtDesc(String diagramId);
    
    List<DatabaseConnection> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<DatabaseConnection> findByIdAndUserId(String id, String userId);
    
    void deleteByIdAndUserId(String id, String userId);
    
    boolean existsByDiagramIdAndName(String diagramId, String name);
}
