package com.chartdb.repository;

import com.chartdb.model.DiagramVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, String> {
    
    // Find all versions for a diagram
    List<DiagramVersion> findByDiagramIdOrderByVersionNumberDesc(String diagramId);
    
    // Find versions with pagination
    Page<DiagramVersion> findByDiagramIdOrderByVersionNumberDesc(String diagramId, Pageable pageable);
    
    // Find current version
    Optional<DiagramVersion> findByDiagramIdAndIsCurrentTrue(String diagramId);
    
    // Find by version number
    Optional<DiagramVersion> findByDiagramIdAndVersionNumber(String diagramId, Integer versionNumber);
    
    // Get latest version number
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DiagramVersion v WHERE v.diagram.id = :diagramId")
    Integer getLatestVersionNumber(@Param("diagramId") String diagramId);
    
    // Clear current flag for all versions
    @Modifying
    @Query("UPDATE DiagramVersion v SET v.isCurrent = false WHERE v.diagram.id = :diagramId")
    void clearCurrentFlag(@Param("diagramId") String diagramId);
    
    // Set current version
    @Modifying
    @Query("UPDATE DiagramVersion v SET v.isCurrent = true WHERE v.id = :versionId")
    void setCurrentVersion(@Param("versionId") String versionId);
    
    // Count versions
    long countByDiagramId(String diagramId);
    
    // Find recent versions
    List<DiagramVersion> findTop10ByDiagramIdOrderByCreatedAtDesc(String diagramId);
}
