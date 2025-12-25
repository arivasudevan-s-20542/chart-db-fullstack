package com.chartdb.mapper;

import com.chartdb.dto.response.PermissionResponse;
import com.chartdb.model.DiagramPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionMapper {
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userDisplayName", source = "user.displayName")
    @Mapping(target = "userAvatarUrl", source = "user.avatarUrl")
    PermissionResponse toResponse(DiagramPermission permission);
    
    List<PermissionResponse> toResponseList(List<DiagramPermission> permissions);
}
