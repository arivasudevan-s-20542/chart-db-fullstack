package com.chartdb.controller;

import com.chartdb.dto.request.CreateColumnRequest;
import com.chartdb.dto.request.UpdateColumnRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.ColumnResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.ColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables/{tableId}/columns")
@RequiredArgsConstructor
public class ColumnController {
    
    private final ColumnService columnService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ColumnResponse>> createColumn(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tableId,
            @Valid @RequestBody CreateColumnRequest request) {
        ColumnResponse response = columnService.createColumn(tableId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Column created", response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ColumnResponse>>> getTableColumns(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tableId) {
        List<ColumnResponse> response = columnService.getTableColumns(tableId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{columnId}")
    public ResponseEntity<ApiResponse<ColumnResponse>> updateColumn(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tableId,
            @PathVariable String columnId,
            @Valid @RequestBody UpdateColumnRequest request) {
        ColumnResponse response = columnService.updateColumn(columnId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Column updated", response));
    }
    
    @DeleteMapping("/{columnId}")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tableId,
            @PathVariable String columnId) {
        columnService.deleteColumn(columnId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Column deleted", null));
    }
    
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderColumns(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String tableId,
            @RequestBody List<String> columnIds) {
        columnService.reorderColumns(tableId, currentUser.getId(), columnIds);
        return ResponseEntity.ok(ApiResponse.success("Columns reordered", null));
    }
}
