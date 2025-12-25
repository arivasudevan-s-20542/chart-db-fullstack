package com.chartdb.mapper;

import com.chartdb.dto.response.CollaboratorResponse;
import com.chartdb.model.ActiveCollaborator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CollaboratorMapper {
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userDisplayName", source = "user.displayName")
    @Mapping(target = "userAvatarUrl", source = "user.avatarUrl")
    @Mapping(target = "cursorColor", source = "user.cursorColor")
    @Mapping(target = "selectedTableId", source = "selectedTable.id")
    @Mapping(target = "selectedTableName", source = "selectedTable.name")
    @Mapping(target = "selectedColumnId", source = "selectedColumn.id")
    CollaboratorResponse toResponse(ActiveCollaborator collaborator);
    
    List<CollaboratorResponse> toResponseList(List<ActiveCollaborator> collaborators);
}
