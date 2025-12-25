package com.chartdb.service;

import com.chartdb.dto.request.CreateNoteRequest;
import com.chartdb.dto.response.NoteResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.NoteMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramNote;
import com.chartdb.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final DiagramService diagramService;

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(String diagramId, String userId) {
        // Verify user has access to the diagram (uses findDiagramById which is read-only)
        Diagram diagram = diagramService.findDiagramById(diagramId);
        if (!diagramService.canUserView(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<DiagramNote> notes = noteRepository.findByDiagramId(diagramId);
        return noteMapper.toResponseList(notes);
    }

    @Transactional
    public NoteResponse createNote(String diagramId, String userId, CreateNoteRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramNote note = noteMapper.toEntity(request);
        note.setDiagram(diagram);
        
        if (note.getId() == null) {
            note.setId(java.util.UUID.randomUUID().toString());
        }
        
        // Set z-index if not provided
        if (note.getZIndex() == null || note.getZIndex() == 0) {
            note.setZIndex(noteRepository.findMaxZIndex(diagramId) + 1);
        }

        note = noteRepository.save(note);
        log.info("Note created: {} in diagram {} by user {}", note.getId(), diagramId, userId);
        
        return noteMapper.toResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(String diagramId, String noteId, String userId, CreateNoteRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramNote note = noteRepository.findById(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));

        noteMapper.updateEntity(request, note);
        note = noteRepository.save(note);
        
        log.info("Note updated: {} in diagram {} by user {}", noteId, diagramId, userId);
        return noteMapper.toResponse(note);
    }

    @Transactional
    public void deleteNote(String diagramId, String noteId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        if (!noteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("Note", "id", noteId);
        }

        noteRepository.deleteById(noteId);
        log.info("Note deleted: {} from diagram {} by user {}", noteId, diagramId, userId);
    }
}
