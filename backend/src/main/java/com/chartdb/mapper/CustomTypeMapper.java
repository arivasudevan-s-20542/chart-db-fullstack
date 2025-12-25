package com.chartdb.mapper;

import com.chartdb.dto.request.CreateCustomTypeRequest;
import com.chartdb.dto.response.CustomTypeResponse;
import com.chartdb.model.DiagramCustomType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomTypeMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiagramCustomType toEntity(CreateCustomTypeRequest request);

    @Mapping(target = "diagramId", source = "diagram.id")
    CustomTypeResponse toResponse(DiagramCustomType entity);

    List<CustomTypeResponse> toResponseList(List<DiagramCustomType> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateCustomTypeRequest request, @MappingTarget DiagramCustomType entity);
}
