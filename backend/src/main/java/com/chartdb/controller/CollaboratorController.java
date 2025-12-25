package com.chartdb.controller;

import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.CollaboratorResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagrams/{diagramId}/collaborators")
@RequiredArgsConstructor
public class CollaboratorController {
    
    private final CollaborationService collaborationService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<CollaboratorResponse>>> getActiveCollaborators(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<CollaboratorResponse> response = collaborationService.getActiveCollaborators(diagramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
