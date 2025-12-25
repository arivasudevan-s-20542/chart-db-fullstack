package com.chartdb.mapper;

import com.chartdb.dto.request.CreateDependencyRequest;
import com.chartdb.dto.response.DependencyResponse;
import com.chartdb.model.DiagramDependency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DependencyMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiagramDependency toEntity(CreateDependencyRequest request);

    @Mapping(target = "diagramId", source = "diagram.id")
    DependencyResponse toResponse(DiagramDependency entity);

    List<DependencyResponse> toResponseList(List<DiagramDependency> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateDependencyRequest request, @MappingTarget DiagramDependency entity);
}
