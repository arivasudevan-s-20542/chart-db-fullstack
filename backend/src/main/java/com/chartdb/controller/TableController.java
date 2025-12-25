package com.chartdb.controller;

import com.chartdb.dto.request.BatchMoveTablesRequest;
import com.chartdb.dto.request.CreateTableRequest;
import com.chartdb.dto.request.MoveTableRequest;
import com.chartdb.dto.request.UpdateTableRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.TableResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagrams/{diagramId}/tables")
@RequiredArgsConstructor
public class TableController {
    
    private final TableService tableService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @Valid @RequestBody CreateTableRequest request) {
        TableResponse response = tableService.createTable(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Table created", response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TableResponse>>> getDiagramTables(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<TableResponse> response = tableService.getDiagramTables(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String tableId) {
        TableResponse response = tableService.getTable(tableId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String tableId,
            @Valid @RequestBody UpdateTableRequest request) {
        TableResponse response = tableService.updateTable(tableId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Table updated", response));
    }
    
    @PatchMapping("/{tableId}/move")
    public ResponseEntity<ApiResponse<TableResponse>> moveTable(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String tableId,
            @Valid @RequestBody MoveTableRequest request) {
        TableResponse response = tableService.moveTable(tableId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PatchMapping("/batch-move")
    public ResponseEntity<ApiResponse<Void>> batchMoveTables(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @Valid @RequestBody BatchMoveTablesRequest request) {
        tableService.batchMoveTables(diagramId, currentUser.getId(), 
            request.getTableIds(), request.getDeltaX(), request.getDeltaY());
        return ResponseEntity.ok(ApiResponse.success("Tables moved", null));
    }
    
    @DeleteMapping("/{tableId}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String tableId) {
        tableService.deleteTable(tableId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Table deleted", null));
    }
}
