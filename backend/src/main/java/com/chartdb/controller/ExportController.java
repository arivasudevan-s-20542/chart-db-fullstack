package com.chartdb.controller;

import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    
    private final ExportService exportService;
    
    /**
     * Export diagram as SQL DDL
     */
    @GetMapping("/diagram/{diagramId}/sql")
    public ResponseEntity<String> exportAsSql(
            @PathVariable String diagramId,
            @RequestParam(defaultValue = "postgresql") String dialect,
            @CurrentUser UserPrincipal currentUser) {
        String sql = exportService.exportAsSql(diagramId, dialect, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"schema.sql\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(sql);
    }
    
    /**
     * Export diagram as JSON
     */
    @GetMapping("/diagram/{diagramId}/json")
    public ResponseEntity<String> exportAsJson(
            @PathVariable String diagramId,
            @CurrentUser UserPrincipal currentUser) {
        String json = exportService.exportAsJson(diagramId, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diagram.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json);
    }
    
    /**
     * Export diagram as PNG (server-side rendering)
     * Note: Currently not implemented - use client-side export
     */
    @GetMapping("/diagram/{diagramId}/png")
    public ResponseEntity<byte[]> exportAsPng(
            @PathVariable String diagramId,
            @RequestParam(defaultValue = "2") int scale,
            @CurrentUser UserPrincipal currentUser) {
        byte[] image = exportService.exportAsPng(diagramId, scale, currentUser.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diagram.png\"")
            .contentType(MediaType.IMAGE_PNG)
            .body(image);
    }
}
