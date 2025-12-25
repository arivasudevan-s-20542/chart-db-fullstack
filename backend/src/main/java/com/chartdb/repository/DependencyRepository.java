package com.chartdb.repository;

import com.chartdb.model.DiagramDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DependencyRepository extends JpaRepository<DiagramDependency, String> {

    List<DiagramDependency> findByDiagramId(String diagramId);

    @Modifying
    @Query("DELETE FROM DiagramDependency d WHERE d.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);

    boolean existsByDiagramIdAndSourceTableIdAndTargetTableId(
        String diagramId, String sourceTableId, String targetTableId);
}
