package com.chartdb.mapper;

import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.response.DiagramResponse;
import com.chartdb.dto.response.DiagramSummaryResponse;
import com.chartdb.model.Diagram;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, 
        uses = {TableMapper.class, RelationshipMapper.class})
public interface DiagramMapper {
    
    // Map id from request if provided, otherwise it will be null and JPA will generate one
    @Mapping(target = "id", source = "id")
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publicSlug", ignore = true)
    @Mapping(target = "tables", ignore = true)
    @Mapping(target = "relationships", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "activeCollaborators", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "auditLogs", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "exportCount", ignore = true)
    @Mapping(target = "lastAccessedAt", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "canvasZoom", ignore = true)
    @Mapping(target = "canvasOffsetX", ignore = true)
    @Mapping(target = "canvasOffsetY", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Diagram toEntity(CreateDiagramRequest request);
    
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerDisplayName", source = "owner.displayName")
    @Mapping(target = "ownerAvatarUrl", source = "owner.avatarUrl")
    @Mapping(target = "status", source = "status")
    DiagramResponse toResponse(Diagram diagram);
    
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerDisplayName", source = "owner.displayName")
    @Mapping(target = "ownerAvatarUrl", source = "owner.avatarUrl")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "tableCount", expression = "java(diagram.getTables() != null ? diagram.getTables().size() : 0)")
    @Mapping(target = "relationshipCount", expression = "java(diagram.getRelationships() != null ? diagram.getRelationships().size() : 0)")
    DiagramSummaryResponse toSummaryResponse(Diagram diagram);
    
    List<DiagramResponse> toResponseList(List<Diagram> diagrams);
    
    List<DiagramSummaryResponse> toSummaryResponseList(List<Diagram> diagrams);
}
