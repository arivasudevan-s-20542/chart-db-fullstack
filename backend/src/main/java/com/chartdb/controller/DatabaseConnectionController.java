package com.chartdb.controller;

import com.chartdb.dto.request.CreateConnectionRequest;
import com.chartdb.dto.request.TestConnectionRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.ConnectionResponse;
import com.chartdb.dto.response.ConnectionTestResult;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.DatabaseConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class DatabaseConnectionController {
    
    private final DatabaseConnectionService connectionService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ConnectionResponse>> createConnection(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateConnectionRequest request) {
        ConnectionResponse response = connectionService.createConnection(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Connection created successfully", response));
    }
    
    @GetMapping("/diagram/{diagramId}")
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getConnectionsByDiagram(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<ConnectionResponse> connections = connectionService.getConnectionsByDiagram(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(connections));
    }
    
    @GetMapping("/{connectionId}")
    public ResponseEntity<ApiResponse<ConnectionResponse>> getConnection(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String connectionId) {
        ConnectionResponse connection = connectionService.getConnection(connectionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(connection));
    }
    
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteConnection(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String connectionId) {
        connectionService.deleteConnection(connectionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Connection deleted successfully", null));
    }
    
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<ConnectionTestResult>> testConnection(
            @Valid @RequestBody TestConnectionRequest request) {
        ConnectionTestResult result = connectionService.testConnection(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PostMapping("/{connectionId}/test")
    public ResponseEntity<ApiResponse<ConnectionTestResult>> testExistingConnection(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String connectionId) {
        ConnectionTestResult result = connectionService.testExistingConnection(connectionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
