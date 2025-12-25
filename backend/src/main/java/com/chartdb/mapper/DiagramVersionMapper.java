package com.chartdb.mapper;

import com.chartdb.dto.response.DiagramVersionResponse;
import com.chartdb.model.DiagramVersion;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DiagramVersionMapper {
    
    @Named("toFullResponse")
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByDisplayName", source = "createdBy.displayName")
    @Mapping(target = "createdByAvatarUrl", source = "createdBy.avatarUrl")
    DiagramVersionResponse toResponse(DiagramVersion version);
    
    @Named("toSummary")
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByDisplayName", source = "createdBy.displayName")
    @Mapping(target = "createdByAvatarUrl", source = "createdBy.avatarUrl")
    @Mapping(target = "snapshotData", ignore = true)
    DiagramVersionResponse toSummaryResponse(DiagramVersion version);
    
    @IterableMapping(qualifiedByName = "toFullResponse")
    List<DiagramVersionResponse> toResponseList(List<DiagramVersion> versions);
}
