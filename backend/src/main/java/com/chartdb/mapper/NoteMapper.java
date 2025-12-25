package com.chartdb.mapper;

import com.chartdb.dto.request.CreateNoteRequest;
import com.chartdb.dto.response.NoteResponse;
import com.chartdb.model.DiagramNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiagramNote toEntity(CreateNoteRequest request);

    @Mapping(target = "diagramId", source = "diagram.id")
    NoteResponse toResponse(DiagramNote entity);

    List<NoteResponse> toResponseList(List<DiagramNote> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "diagram", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateNoteRequest request, @MappingTarget DiagramNote entity);
}
