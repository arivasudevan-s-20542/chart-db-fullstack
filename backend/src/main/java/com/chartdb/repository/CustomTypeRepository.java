package com.chartdb.repository;

import com.chartdb.model.DiagramCustomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomTypeRepository extends JpaRepository<DiagramCustomType, String> {

    List<DiagramCustomType> findByDiagramId(String diagramId);

    Optional<DiagramCustomType> findByDiagramIdAndName(String diagramId, String name);

    @Modifying
    @Query("DELETE FROM DiagramCustomType ct WHERE ct.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);

    boolean existsByDiagramIdAndName(String diagramId, String name);
}
