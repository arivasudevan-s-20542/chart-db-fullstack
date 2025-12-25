package com.chartdb.controller;

import com.chartdb.dto.request.CreateRelationshipRequest;
import com.chartdb.dto.request.UpdateRelationshipRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.RelationshipResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.RelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagrams/{diagramId}/relationships")
@RequiredArgsConstructor
public class RelationshipController {
    
    private final RelationshipService relationshipService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<RelationshipResponse>> createRelationship(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @Valid @RequestBody CreateRelationshipRequest request) {
        RelationshipResponse response = relationshipService.createRelationship(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Relationship created", response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<RelationshipResponse>>> getDiagramRelationships(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<RelationshipResponse> response = relationshipService.getDiagramRelationships(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{relationshipId}")
    public ResponseEntity<ApiResponse<RelationshipResponse>> updateRelationship(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String relationshipId,
            @Valid @RequestBody UpdateRelationshipRequest request) {
        RelationshipResponse response = relationshipService.updateRelationship(relationshipId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Relationship updated", response));
    }
    
    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<ApiResponse<Void>> deleteRelationship(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String relationshipId) {
        relationshipService.deleteRelationship(relationshipId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Relationship deleted", null));
    }
}
