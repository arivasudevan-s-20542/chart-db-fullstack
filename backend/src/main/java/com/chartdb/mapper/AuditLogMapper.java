package com.chartdb.mapper;

import com.chartdb.dto.response.AuditLogResponse;
import com.chartdb.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuditLogMapper {
    
    @Mapping(target = "diagramId", source = "diagram.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userDisplayName", source = "user.displayName")
    @Mapping(target = "userAvatarUrl", source = "user.avatarUrl")
    AuditLogResponse toResponse(AuditLog auditLog);
    
    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);
}
