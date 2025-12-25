package com.chartdb.mapper;

import com.chartdb.dto.request.CreateAreaRequest;
import com.chartdb.dto.response.AreaResponse;
import com.chartdb.model.DiagramArea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AreaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiagramArea toEntity(CreateAreaRequest request);

    @Mapping(target = "diagramId", source = "diagram.id")
    AreaResponse toResponse(DiagramArea entity);

    List<AreaResponse> toResponseList(List<DiagramArea> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateAreaRequest request, @MappingTarget DiagramArea entity);
}
