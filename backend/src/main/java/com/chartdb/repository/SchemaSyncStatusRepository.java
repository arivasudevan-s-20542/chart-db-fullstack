package com.chartdb.repository;

import com.chartdb.model.SchemaSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemaSyncStatusRepository extends JpaRepository<SchemaSyncStatus, String> {
    
    Optional<SchemaSyncStatus> findByDiagramId(String diagramId);
}
