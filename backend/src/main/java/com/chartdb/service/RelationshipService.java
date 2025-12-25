package com.chartdb.service;

import com.chartdb.dto.request.CreateRelationshipRequest;
import com.chartdb.dto.request.UpdateRelationshipRequest;
import com.chartdb.dto.response.RelationshipResponse;
import com.chartdb.exception.AccessDeniedException;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.RelationshipMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramTable;
import com.chartdb.model.Relationship;
import com.chartdb.model.TableColumn;
import com.chartdb.repository.ColumnRepository;
import com.chartdb.repository.RelationshipRepository;
import com.chartdb.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelationshipService {
    
    private final RelationshipRepository relationshipRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final DiagramService diagramService;
    private final RelationshipMapper relationshipMapper;
    
    @Transactional
    public RelationshipResponse createRelationship(String diagramId, String userId, CreateRelationshipRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this diagram");
        }
        
        DiagramTable sourceTable = tableRepository.findById(request.getSourceTableId())
            .orElseThrow(() -> new BadRequestException("Source table not found"));
        DiagramTable targetTable = tableRepository.findById(request.getTargetTableId())
            .orElseThrow(() -> new BadRequestException("Target table not found"));
        
        // Verify tables belong to the same diagram
        if (!sourceTable.getDiagram().getId().equals(diagramId) || 
            !targetTable.getDiagram().getId().equals(diagramId)) {
            throw new BadRequestException("Tables must belong to the same diagram");
        }
        
        Relationship relationship = relationshipMapper.toEntity(request);
        relationship.setDiagram(diagram);
        relationship.setSourceTable(sourceTable);
        relationship.setTargetTable(targetTable);
        
        // Set columns if provided (use effective methods to support both field naming conventions)
        String effectiveSourceColumnId = request.getEffectiveSourceColumnId();
        if (effectiveSourceColumnId != null) {
            TableColumn sourceColumn = columnRepository.findById(effectiveSourceColumnId)
                .orElseThrow(() -> new BadRequestException("Source column not found"));
            relationship.setSourceColumn(sourceColumn);
        }
        
        String effectiveTargetColumnId = request.getEffectiveTargetColumnId();
        if (effectiveTargetColumnId != null) {
            TableColumn targetColumn = columnRepository.findById(effectiveTargetColumnId)
                .orElseThrow(() -> new BadRequestException("Target column not found"));
            relationship.setTargetColumn(targetColumn);
        }
        
        relationship = relationshipRepository.save(relationship);
        log.info("Relationship created: {} in diagram {} by user {}", relationship.getId(), diagramId, userId);
        
        return relationshipMapper.toResponse(relationship);
    }
    
    @Transactional(readOnly = true)
    public List<RelationshipResponse> getDiagramRelationships(String diagramId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<Relationship> relationships = relationshipRepository.findByDiagramIdWithTables(diagramId);
        return relationshipMapper.toResponseList(relationships);
    }
    
    @Transactional
    public RelationshipResponse updateRelationship(String relationshipId, String userId, UpdateRelationshipRequest request) {
        Relationship relationship = findRelationshipById(relationshipId);
        Diagram diagram = relationship.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this relationship");
        }
        
        if (request.getName() != null) relationship.setName(request.getName());
        if (request.getRelationshipType() != null) relationship.setRelationshipType(request.getRelationshipType());
        if (request.getSourceHandle() != null) relationship.setSourceHandle(request.getSourceHandle());
        if (request.getTargetHandle() != null) relationship.setTargetHandle(request.getTargetHandle());
        if (request.getPathPoints() != null) relationship.setPathPoints(request.getPathPoints());
        if (request.getSourceCardinality() != null) relationship.setCardinalitySource(request.getSourceCardinality());
        if (request.getTargetCardinality() != null) relationship.setCardinalityTarget(request.getTargetCardinality());
        
        relationship = relationshipRepository.save(relationship);
        return relationshipMapper.toResponse(relationship);
    }
    
    @Transactional
    public void deleteRelationship(String relationshipId, String userId) {
        Relationship relationship = findRelationshipById(relationshipId);
        Diagram diagram = relationship.getDiagram();
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to delete this relationship");
        }
        
        relationshipRepository.delete(relationship);
        log.info("Relationship deleted: {} from diagram {} by user {}", relationshipId, diagram.getId(), userId);
    }
    
    public Relationship findRelationshipById(String relationshipId) {
        return relationshipRepository.findByIdWithTables(relationshipId)
            .orElseThrow(() -> new ResourceNotFoundException("Relationship", "id", relationshipId));
    }
}
