package com.chartdb.controller;

import com.chartdb.dto.request.CreateCustomTypeRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.CustomTypeResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.CustomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing diagram custom types.
 * Custom types represent user-defined data types like enums, composite types, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/diagrams/{diagramId}/custom-types")
@RequiredArgsConstructor
public class CustomTypeController {

    private final CustomTypeService customTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomTypeResponse>>> getCustomTypes(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<CustomTypeResponse> customTypes = customTypeService.getCustomTypes(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(customTypes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomTypeResponse>> createCustomType(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @RequestBody CreateCustomTypeRequest request) {
        CustomTypeResponse customType = customTypeService.createCustomType(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Custom type created", customType));
    }

    @PutMapping("/{customTypeId}")
    public ResponseEntity<ApiResponse<CustomTypeResponse>> updateCustomType(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String customTypeId,
            @RequestBody CreateCustomTypeRequest request) {
        CustomTypeResponse customType = customTypeService.updateCustomType(diagramId, customTypeId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Custom type updated", customType));
    }

    @DeleteMapping("/{customTypeId}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomType(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String customTypeId) {
        customTypeService.deleteCustomType(diagramId, customTypeId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Custom type deleted", null));
    }
}
