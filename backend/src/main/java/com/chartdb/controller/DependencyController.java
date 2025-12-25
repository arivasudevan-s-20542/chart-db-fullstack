package com.chartdb.controller;

import com.chartdb.dto.request.CreateDependencyRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.DependencyResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.DependencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing diagram dependencies.
 * Dependencies represent relationships between database objects like views depending on tables.
 */
@Slf4j
@RestController
@RequestMapping("/api/diagrams/{diagramId}/dependencies")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DependencyResponse>>> getDependencies(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<DependencyResponse> dependencies = dependencyService.getDependencies(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(dependencies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DependencyResponse>> createDependency(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @RequestBody CreateDependencyRequest request) {
        DependencyResponse dependency = dependencyService.createDependency(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Dependency created", dependency));
    }

    @PutMapping("/{dependencyId}")
    public ResponseEntity<ApiResponse<DependencyResponse>> updateDependency(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String dependencyId,
            @RequestBody CreateDependencyRequest request) {
        DependencyResponse dependency = dependencyService.updateDependency(diagramId, dependencyId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Dependency updated", dependency));
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String dependencyId) {
        dependencyService.deleteDependency(diagramId, dependencyId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Dependency deleted", null));
    }
}
