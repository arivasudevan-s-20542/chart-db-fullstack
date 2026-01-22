package com.chartdb.controller;

import com.chartdb.dto.request.ExecuteQueryRequest;
import com.chartdb.dto.request.SaveQueryRequest;
import com.chartdb.dto.response.*;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.QueryExecutionService;
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
@RequestMapping("/api/queries")
@RequiredArgsConstructor
public class QueryController {
    
    private final QueryExecutionService queryExecutionService;
    
    @PostMapping("/execute/{connectionId}")
    public ResponseEntity<ApiResponse<QueryExecutionResult>> executeQuery(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String connectionId,
            @Valid @RequestBody ExecuteQueryRequest request) {
        QueryExecutionResult result = queryExecutionService.executeQuery(connectionId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<QueryHistoryResponse>>> getQueryHistory(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<QueryHistoryResponse> history = queryExecutionService.getQueryHistory(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    @PostMapping("/saved")
    public ResponseEntity<ApiResponse<SavedQueryResponse>> saveQuery(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody SaveQueryRequest request) {
        SavedQueryResponse response = queryExecutionService.saveQuery(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Query saved successfully", response));
    }
    
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedQueryResponse>>> getSavedQueries(
            @CurrentUser UserPrincipal currentUser) {
        List<SavedQueryResponse> queries = queryExecutionService.getSavedQueries(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(queries));
    }
}
