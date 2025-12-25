package com.chartdb.dto.request;

import com.chartdb.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {
    
    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}
