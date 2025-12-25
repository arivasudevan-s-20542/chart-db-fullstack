package com.chartdb.controller;

import com.chartdb.dto.request.CreateNoteRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.NoteResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing diagram notes.
 * Notes are text annotations that can be placed anywhere on the canvas.
 */
@Slf4j
@RestController
@RequestMapping("/api/diagrams/{diagramId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getNotes(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId) {
        List<NoteResponse> notes = noteService.getNotes(diagramId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @RequestBody CreateNoteRequest request) {
        NoteResponse note = noteService.createNote(diagramId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Note created", note));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String noteId,
            @RequestBody CreateNoteRequest request) {
        NoteResponse note = noteService.updateNote(diagramId, noteId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Note updated", note));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String diagramId,
            @PathVariable String noteId) {
        noteService.deleteNote(diagramId, noteId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Note deleted", null));
    }
}
