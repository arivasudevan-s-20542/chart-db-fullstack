package com.chartdb.service;

import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.request.UpdateDiagramRequest;
import com.chartdb.dto.response.*;
import com.chartdb.exception.AccessDeniedException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.DiagramMapper;
import com.chartdb.mapper.RelationshipMapper;
import com.chartdb.mapper.TableMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramTable;
import com.chartdb.model.Relationship;
import com.chartdb.model.User;
import com.chartdb.model.enums.DiagramStatus;
import com.chartdb.model.enums.PermissionLevel;
import com.chartdb.repository.DiagramRepository;
import com.chartdb.repository.RelationshipRepository;
import com.chartdb.repository.TableRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagramService {
    
    private final DiagramRepository diagramRepository;
    private final TableRepository tableRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final DiagramMapper diagramMapper;
    private final TableMapper tableMapper;
    private final RelationshipMapper relationshipMapper;
    private final PermissionService permissionService;
    
    @Transactional
    public DiagramResponse createDiagram(String userId, CreateDiagramRequest request) {
        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Diagram diagram = diagramMapper.toEntity(request);
        diagram.setOwner(owner);
        diagram.setStatus(DiagramStatus.ACTIVE);
        diagram.setCanvasZoom(BigDecimal.ONE);
        diagram.setCanvasOffsetX(BigDecimal.ZERO);
        diagram.setCanvasOffsetY(BigDecimal.ZERO);
        diagram.setViewCount(0);
        diagram.setExportCount(0);
        diagram.setLastAccessedAt(Instant.now());
        
        if (Boolean.TRUE.equals(request.getIsPublic())) {
            diagram.setPublicSlug(generateUniqueSlug());
        }
        
        diagram = diagramRepository.save(diagram);
        
        // Create owner permission
        permissionService.createOwnerPermission(diagram, owner);
        
        log.info("Diagram created: {} by user {}", diagram.getId(), userId);
        return diagramMapper.toResponse(diagram);
    }
    
    @Transactional
    public DiagramResponse getDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramById(diagramId);
        
        if (!canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this diagram");
        }
        
        diagramRepository.updateLastAccessed(diagramId, Instant.now());
        
        return diagramMapper.toResponse(diagram);
    }
    
    /**
     * Get full diagram with all tables, columns, and relationships.
     * This is the main endpoint for loading a diagram in the editor.
     */
    @Transactional
    public DiagramFullResponse getFullDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramById(diagramId);
        
        if (!canUserView(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to view this diagram");
        }
        
        // Load tables with columns
        List<DiagramTable> tables = tableRepository.findByDiagramIdWithColumns(diagramId);
        List<Relationship> relationships = relationshipRepository.findByDiagramId(diagramId);
        
        // Get user's permission level
        PermissionLevel permissionLevel = permissionService.getPermissionLevel(diagramId, userId);
        
        // Update last accessed
        diagramRepository.updateLastAccessed(diagramId, Instant.now());
        
        return DiagramFullResponse.builder()
            .id(diagram.getId())
            .name(diagram.getName())
            .description(diagram.getDescription())
            .databaseType(diagram.getDatabaseType())
            .isPublic(diagram.getIsPublic())
            .zoom(diagram.getCanvasZoom())
            .offsetX(diagram.getCanvasOffsetX())
            .offsetY(diagram.getCanvasOffsetY())
            .tableCount(tables.size())
            .relationshipCount(relationships.size())
            .ownerId(diagram.getOwner().getId())
            .ownerDisplayName(diagram.getOwner().getDisplayName())
            .createdAt(diagram.getCreatedAt())
            .updatedAt(diagram.getUpdatedAt())
            .tables(tableMapper.toResponseList(tables))
            .relationships(relationshipMapper.toResponseList(relationships))
            .permissionLevel(permissionLevel != null ? permissionLevel.name() : "VIEWER")
            .build();
    }

    @Transactional(readOnly = true)
    public Page<DiagramSummaryResponse> getUserDiagrams(String userId, Pageable pageable) {
        Page<Diagram> diagrams = diagramRepository.findAccessibleByUser(userId, DiagramStatus.ACTIVE, pageable);
        return diagrams.map(diagramMapper::toSummaryResponse);
    }
    
    @Transactional(readOnly = true)
    public List<DiagramSummaryResponse> getRecentDiagrams(String userId, int limit) {
        List<Diagram> diagrams = diagramRepository.findRecentlyAccessed(
            userId, DiagramStatus.ACTIVE, Pageable.ofSize(limit));
        return diagramMapper.toSummaryResponseList(diagrams);
    }
    
    @Transactional
    public DiagramResponse updateDiagram(String diagramId, String userId, UpdateDiagramRequest request) {
        Diagram diagram = findDiagramById(diagramId);
        
        if (!canUserEdit(diagram, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this diagram");
        }
        
        if (request.getName() != null) diagram.setName(request.getName());
        if (request.getDescription() != null) diagram.setDescription(request.getDescription());
        if (request.getDatabaseType() != null) diagram.setDatabaseType(request.getDatabaseType());
        // Note: metadata field can be added to Diagram entity if needed
        
        if (request.getIsPublic() != null) {
            diagram.setIsPublic(request.getIsPublic());
            if (request.getIsPublic() && diagram.getPublicSlug() == null) {
                diagram.setPublicSlug(generateUniqueSlug());
            }
        }
        
        if (request.getCanvasZoom() != null) diagram.setCanvasZoom(request.getCanvasZoom());
        if (request.getCanvasOffsetX() != null) diagram.setCanvasOffsetX(request.getCanvasOffsetX());
        if (request.getCanvasOffsetY() != null) diagram.setCanvasOffsetY(request.getCanvasOffsetY());
        
        diagram = diagramRepository.save(diagram);
        log.info("Diagram updated: {} by user {}", diagramId, userId);
        
        return diagramMapper.toResponse(diagram);
    }
    
    @Transactional
    public void deleteDiagram(String diagramId, String userId) {
        Diagram diagram = findDiagramById(diagramId);
        
        if (!isOwner(diagram, userId)) {
            throw new AccessDeniedException("Only the owner can delete this diagram");
        }
        
        diagramRepository.archiveDiagram(diagramId, Instant.now());
        log.info("Diagram archived: {} by user {}", diagramId, userId);
    }
    
    @Transactional(readOnly = true)
    public DiagramResponse getPublicDiagram(String slug) {
        Diagram diagram = diagramRepository.findByPublicSlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "slug", slug));
        
        if (!Boolean.TRUE.equals(diagram.getIsPublic())) {
            throw new ResourceNotFoundException("Diagram", "slug", slug);
        }
        
        diagramRepository.incrementViewCount(diagram.getId());
        return diagramMapper.toResponse(diagram);
    }
    
    public Diagram findDiagramById(String diagramId) {
        return diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "id", diagramId));
    }
    
    public boolean canUserView(Diagram diagram, String userId) {
        if (Boolean.TRUE.equals(diagram.getIsPublic())) return true;
        if (isOwner(diagram, userId)) return true;
        return permissionService.hasPermission(diagram.getId(), userId, PermissionLevel.VIEWER);
    }
    
    public boolean canUserEdit(Diagram diagram, String userId) {
        if (isOwner(diagram, userId)) return true;
        return permissionService.hasPermission(diagram.getId(), userId, PermissionLevel.EDITOR);
    }
    
    public boolean isOwner(Diagram diagram, String userId) {
        return diagram.getOwner().getId().equals(userId);
    }
    
    private String generateUniqueSlug() {
        String slug;
        do {
            slug = UUID.randomUUID().toString().substring(0, 8);
        } while (diagramRepository.existsByPublicSlug(slug));
        return slug;
    }
}
