package com.chartdb.mapper;

import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.dto.response.TableResponse;
import com.chartdb.model.DiagramTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ColumnMapper.class})
public interface TableMapper {
    
    // Map id from request if provided, otherwise it will be null and JPA will generate one
    @Mapping(target = "id", source = "id")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "columns", ignore = true)
    @Mapping(target = "incomingRelationships", ignore = true)
    @Mapping(target = "outgoingRelationships", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "zIndex", ignore = true)
    @Mapping(target = "isHidden", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "indexesJson", ignore = true)
    DiagramTable toEntity(CreateTableRequest request);
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "indexes", source = "indexesJson")
    TableResponse toResponse(DiagramTable table);
    
    List<TableResponse> toResponseList(List<DiagramTable> tables);
}
