package com.chartdb.controller;

import com.chartdb.dto.request.CreateDiagramRequest;
import com.chartdb.dto.request.ShareDiagramRequest;
import com.chartdb.dto.request.UpdateDiagramRequest;
import com.chartdb.dto.response.*;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.DiagramService;
import com.chartdb.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagrams")
@RequiredArgsConstructor
public class DiagramController {
    
    private final DiagramService diagramService;
    private final PermissionService permissionService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<DiagramResponse>> createDiagram(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateDiagramRequest request) {
        DiagramResponse response = diagramService.createDiagram(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Diagram created", response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DiagramSummaryResponse>>> getUserDiagrams(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DiagramSummaryResponse> response = diagramService.getUserDiagrams(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<DiagramSummaryResponse>>> getRecentDiagrams(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "10") int limit) {
        List<DiagramSummaryResponse> response = diagramService.getRecentDiagrams(currentUser.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{diagramId}")
    public ResponseEntity<ApiResponse<DiagramResponse>> getDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        DiagramResponse response = diagramService.getDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get full diagram with all tables, columns, and relationships.
     * This is the main endpoint for loading a diagram in the editor.
     */
    @GetMapping("/{diagramId}/full")
    public ResponseEntity<ApiResponse<DiagramFullResponse>> getFullDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        DiagramFullResponse response = diagramService.getFullDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{diagramId}")
    public ResponseEntity<ApiResponse<DiagramResponse>> updateDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @Valid @RequestBody UpdateDiagramRequest request) {
        DiagramResponse response = diagramService.updateDiagram(diagramId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Diagram updated", response));
    }
    
    @DeleteMapping("/{diagramId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        diagramService.deleteDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Diagram deleted", null));
    }
    
    // Sharing endpoints
    @PostMapping("/{diagramId}/share")
    public ResponseEntity<ApiResponse<PermissionResponse>> shareDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @Valid @RequestBody ShareDiagramRequest request) {
        PermissionResponse response = permissionService.shareDiagram(diagramId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Diagram shared", response));
    }
    
    @GetMapping("/{diagramId}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getDiagramPermissions(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<PermissionResponse> response = permissionService.getDiagramPermissions(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{diagramId}/permissions/{userId}")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String userId) {
        permissionService.removePermission(diagramId, currentUser.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Permission removed", null));
    }
}
