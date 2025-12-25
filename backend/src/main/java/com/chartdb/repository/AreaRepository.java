package com.chartdb.repository;

import com.chartdb.model.DiagramArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<DiagramArea, String> {

    List<DiagramArea> findByDiagramIdOrderBySortOrder(String diagramId);

    @Modifying
    @Query("DELETE FROM DiagramArea a WHERE a.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);

    @Query("SELECT COALESCE(MAX(a.sortOrder), 0) FROM DiagramArea a WHERE a.diagram.id = :diagramId")
    int findMaxSortOrder(@Param("diagramId") String diagramId);

    @Query("SELECT COALESCE(MAX(a.zIndex), 0) FROM DiagramArea a WHERE a.diagram.id = :diagramId")
    int findMaxZIndex(@Param("diagramId") String diagramId);
}
