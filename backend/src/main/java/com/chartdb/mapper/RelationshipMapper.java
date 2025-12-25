package com.chartdb.mapper;

import com.chartdb.dto.request.CreateRelationshipRequest;
import com.chartdb.dto.response.RelationshipResponse;
import com.chartdb.model.Relationship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RelationshipMapper {
    
    // Map id from request if provided, otherwise it will be null and JPA will generate one
    @Mapping(target = "id", source = "id")
    @Mapping(target = "relationshipType", source = "request", qualifiedByName = "toEffectiveRelationshipType")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "sourceTable", ignore = true)
    @Mapping(target = "targetTable", ignore = true)
    @Mapping(target = "sourceColumn", ignore = true)
    @Mapping(target = "targetColumn", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Relationship toEntity(CreateRelationshipRequest request);
    
    @Named("toEffectiveRelationshipType")
    default com.chartdb.model.enums.RelationshipType toEffectiveRelationshipType(CreateRelationshipRequest request) {
        return request.getEffectiveRelationshipType();
    }
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "sourceTableId", source = "sourceTable.id")
    @Mapping(target = "sourceTableName", source = "sourceTable.name")
    @Mapping(target = "sourceColumnId", source = "sourceColumn.id")
    @Mapping(target = "sourceColumnName", source = "sourceColumn.name")
    @Mapping(target = "targetTableId", source = "targetTable.id")
    @Mapping(target = "targetTableName", source = "targetTable.name")
    @Mapping(target = "targetColumnId", source = "targetColumn.id")
    @Mapping(target = "targetColumnName", source = "targetColumn.name")
    @Mapping(target = "sourceCardinality", source = "cardinalitySource")
    @Mapping(target = "targetCardinality", source = "cardinalityTarget")
    RelationshipResponse toResponse(Relationship relationship);
    
    List<RelationshipResponse> toResponseList(List<Relationship> relationships);
}
