package com.chartdb.mapper;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.response.ColumnResponse;
import com.chartdb.model.TableColumn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ColumnMapper {
    
    // Map id from request if provided, otherwise it will be null and JPA will generate one
    @Mapping(target = "id", source = "id")
    @Mapping(target = "dataType", source = "request", qualifiedByName = "toEffectiveDataType")
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "fkTable", ignore = true)
    @Mapping(target = "fkColumn", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TableColumn toEntity(CreateColumnRequest request);
    
    @Named("toEffectiveDataType")
    default String toEffectiveDataType(CreateColumnRequest request) {
        return request.getEffectiveDataType();
    }
    
    @Mapping(target = "tableId", source = "table.id")
    @Mapping(target = "fkTableId", source = "fkTable.id")
    @Mapping(target = "fkTableName", source = "fkTable.name")
    @Mapping(target = "fkColumnId", source = "fkColumn.id")
    @Mapping(target = "fkColumnName", source = "fkColumn.name")
    ColumnResponse toResponse(TableColumn column);
    
    List<ColumnResponse> toResponseList(List<TableColumn> columns);
}
