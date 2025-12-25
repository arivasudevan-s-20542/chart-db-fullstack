package com.chartdb.repository;

import com.chartdb.model.DiagramNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<DiagramNote, String> {

    List<DiagramNote> findByDiagramId(String diagramId);

    @Modifying
    @Query("DELETE FROM DiagramNote n WHERE n.diagram.id = :diagramId")
    void deleteByDiagramId(@Param("diagramId") String diagramId);

    @Query("SELECT COALESCE(MAX(n.zIndex), 0) FROM DiagramNote n WHERE n.diagram.id = :diagramId")
    int findMaxZIndex(@Param("diagramId") String diagramId);
}
