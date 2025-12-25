package com.chartdb.controller;

import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.DiagramResponse;
import com.chartdb.service.DiagramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    
    private final DiagramService diagramService;
    
    @GetMapping("/diagrams/{slug}")
    public ResponseEntity<ApiResponse<DiagramResponse>> getPublicDiagram(@PathVariable String slug) {
        DiagramResponse response = diagramService.getPublicDiagram(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
