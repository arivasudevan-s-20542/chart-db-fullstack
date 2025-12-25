package com.chartdb.controller;

import com.chartdb.dto.request.CreateAreaRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.AreaResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.AreaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing diagram areas.
 * Areas are visual groupings/containers for tables on the canvas.
 */
@Slf4j
@RestController
@RequestMapping("/api/diagrams/{diagramId}/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AreaResponse>>> getAreas(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<AreaResponse> areas = areaService.getAreas(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(areas));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AreaResponse>> createArea(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @RequestBody CreateAreaRequest request) {
        AreaResponse area = areaService.createArea(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Area created", area));
    }

    @PutMapping("/{areaId}")
    public ResponseEntity<ApiResponse<AreaResponse>> updateArea(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String areaId,
            @RequestBody CreateAreaRequest request) {
        AreaResponse area = areaService.updateArea(diagramId, areaId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Area updated", area));
    }

    @DeleteMapping("/{areaId}")
    public ResponseEntity<ApiResponse<Void>> deleteArea(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String areaId) {
        areaService.deleteArea(diagramId, areaId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Area deleted", null));
    }
}
