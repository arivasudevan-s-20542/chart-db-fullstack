package com.chartdb.service;

import com.chartdb.dto.response.CollaboratorResponse;
import com.chartdb.dto.websocket.PresenceMessage;
import com.chartdb.model.*;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.CollaboratorMapper;
import com.chartdb.repository.ActiveCollaboratorRepository;
import com.chartdb.repository.TableRepository;
import com.chartdb.repository.TableLockRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationService {
    
    private final ActiveCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final TableLockRepository lockRepository;
    private final DiagramService diagramService;
    private final CollaboratorMapper collaboratorMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final long SESSION_TIMEOUT_SECONDS = 60;
    private static final long IDLE_TIMEOUT_SECONDS = 300;
    private static final long LOCK_TIMEOUT_SECONDS = 120;
    
    // ═══════════════════════════════════════════════════════════════
    // COLLABORATION SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════
    
    @Transactional
    public CollaboratorResponse joinDiagram(String diagramId, String userId, String websocketSessionId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if already in diagram
        ActiveCollaborator existing = collaboratorRepository
            .findByDiagramIdAndUserId(diagramId, userId)
            .orElse(null);
        
        if (existing != null) {
            existing.setWebsocketSessionId(websocketSessionId);
            existing.setIsActive(true);
            existing.setLastSeen(Instant.now());
            existing = collaboratorRepository.save(existing);
            return collaboratorMapper.toResponse(existing);
        }
        
        // Assign cursor color based on number of existing collaborators
        String cursorColor = assignCursorColor(diagramId);
        
        ActiveCollaborator collaborator = ActiveCollaborator.builder()
            .diagram(diagram)
            .user(user)
            .sessionId(UUID.randomUUID().toString())
            .websocketSessionId(websocketSessionId)
            .cursorColor(cursorColor)
            .cursorX(BigDecimal.ZERO)
            .cursorY(BigDecimal.ZERO)
            .isActive(true)
            .isIdle(false)
            .status("ACTIVE")
            .connectedAt(Instant.now())
            .lastSeen(Instant.now())
            .lastActivity(Instant.now())
            .build();
        
        collaborator = collaboratorRepository.save(collaborator);
        log.info("User {} joined diagram {} with cursor color {}", userId, diagramId, cursorColor);
        
        // Broadcast join event
        broadcastPresence(diagramId, "JOIN", collaboratorMapper.toResponse(collaborator));
        
        return collaboratorMapper.toResponse(collaborator);
    }
    
    @Transactional
    public void leaveDiagram(String diagramId, String userId) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(collaborator -> {
                CollaboratorResponse response = collaboratorMapper.toResponse(collaborator);
                
                // Release any locks held by this user
                lockRepository.deleteByDiagramIdAndLockedByUserId(diagramId, userId);
                
                collaboratorRepository.delete(collaborator);
                log.info("User {} left diagram {}", userId, diagramId);
                
                // Broadcast leave event
                broadcastPresence(diagramId, "LEAVE", response);
            });
    }
    
    @Transactional
    public void leaveByWebsocketSession(String websocketSessionId) {
        collaboratorRepository.findByWebsocketSessionId(websocketSessionId)
            .ifPresent(collaborator -> {
                String diagramId = collaborator.getDiagram().getId();
                String userId = collaborator.getUser().getId();
                CollaboratorResponse response = collaboratorMapper.toResponse(collaborator);
                
                // Release any locks held by this user
                lockRepository.deleteByDiagramIdAndLockedByUserId(diagramId, userId);
                
                collaboratorRepository.delete(collaborator);
                log.info("User {} disconnected from diagram {}", userId, diagramId);
                
                broadcastPresence(diagramId, "LEAVE", response);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CURSOR AND SELECTION TRACKING
    // ═══════════════════════════════════════════════════════════════
    
    @Transactional
    public void updateCursorPosition(String diagramId, String userId, BigDecimal x, BigDecimal y) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(collaborator -> {
                collaboratorRepository.updateCursorPosition(collaborator.getId(), x, y, Instant.now());
            });
    }
    
    @Transactional
    public void updateSelection(String diagramId, String userId, String tableId, String columnId) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(collaborator -> {
                if (tableId != null) {
                    DiagramTable table = tableRepository.findById(tableId).orElse(null);
                    collaborator.setSelectedTable(table);
                } else {
                    collaborator.setSelectedTable(null);
                }
                collaborator.setLastActivity(Instant.now());
                collaboratorRepository.save(collaborator);
            });
    }
    
    @Transactional(readOnly = true)
    public List<CollaboratorResponse> getActiveCollaborators(String diagramId) {
        List<ActiveCollaborator> collaborators = collaboratorRepository.findByDiagramIdAndIsActiveTrue(diagramId);
        return collaboratorMapper.toResponseList(collaborators);
    }
    
    @Transactional(readOnly = true)
    public String getUserCursorColor(String diagramId, String userId) {
        return collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .map(ActiveCollaborator::getCursorColor)
            .orElse("#6366f1"); // default indigo
    }
    
    @Transactional
    public void markIdle(String diagramId, String userId, boolean idle) {
        collaboratorRepository.findByDiagramIdAndUserId(diagramId, userId)
            .ifPresent(collaborator -> {
                collaboratorRepository.updateIdleStatus(
                    collaborator.getId(), 
                    idle, 
                    idle ? "IDLE" : "ACTIVE"
                );
                
                // Broadcast idle status change
                CollaboratorResponse response = collaboratorMapper.toResponse(collaborator);
                response.setIsIdle(idle);
                response.setStatus(idle ? "IDLE" : "ACTIVE");
                broadcastPresence(diagramId, "UPDATE", response);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TABLE LOCKING MECHANISM
    // ═══════════════════════════════════════════════════════════════
    
    @Transactional
    public boolean lockTable(String diagramId, String tableId, String userId) {
        // Check if already locked by another user
        Optional<TableLock> existingLock = lockRepository.findByTableId(tableId);
        
        if (existingLock.isPresent()) {
            TableLock lock = existingLock.get();
            
            // If locked by same user, extend the lock
            if (lock.getUser().getId().equals(userId)) {
                lock.setExpiresAt(Instant.now().plusSeconds(LOCK_TIMEOUT_SECONDS));
                lockRepository.save(lock);
                return true;
            }
            
            // If lock has expired, release it
            if (lock.getExpiresAt().isBefore(Instant.now())) {
                lockRepository.delete(lock);
            } else {
                // Lock is held by another user and not expired
                log.info("Table {} is locked by user {}", tableId, lock.getUser().getId());
                return false;
            }
        }
        
        // Create new lock
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        DiagramTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new ResourceNotFoundException("Table", "id", tableId));
        
        TableLock lock = TableLock.builder()
            .table(table)
            .user(user)
            .acquiredAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LOCK_TIMEOUT_SECONDS))
            .build();
        
        lockRepository.save(lock);
        log.info("User {} acquired lock on table {}", userId, tableId);
        
        return true;
    }
    
    @Transactional
    public void unlockTable(String diagramId, String tableId, String userId) {
        lockRepository.findByTableIdAndLockedByUserId(tableId, userId)
            .ifPresent(lock -> {
                lockRepository.delete(lock);
                log.info("User {} released lock on table {}", userId, tableId);
            });
    }
    
    @Transactional
    public void unlockTable(String diagramId, String tableId) {
        lockRepository.deleteByTableId(tableId);
        log.info("Released all locks on table {}", tableId);
    }
    
    @Transactional(readOnly = true)
    public boolean isTableLocked(String tableId) {
        return lockRepository.existsByTableIdAndExpiresAtAfter(tableId, Instant.now());
    }
    
    @Transactional(readOnly = true)
    public Optional<TableLock> getTableLock(String tableId) {
        return lockRepository.findByTableIdAndExpiresAtAfter(tableId, Instant.now());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SCHEDULED CLEANUP
    // ═══════════════════════════════════════════════════════════════
    
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanupStaleSessions() {
        Instant staleThreshold = Instant.now().minus(SESSION_TIMEOUT_SECONDS, ChronoUnit.SECONDS);
        collaboratorRepository.deleteStaleSessions(staleThreshold);
        
        // Also cleanup expired locks
        lockRepository.deleteExpiredLocks(Instant.now());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════
    
    private String assignCursorColor(String diagramId) {
        // Predefined cursor colors for collaborators
        String[] colors = {
            "#ef4444", // red
            "#f97316", // orange
            "#eab308", // yellow
            "#22c55e", // green
            "#06b6d4", // cyan
            "#3b82f6", // blue
            "#8b5cf6", // violet
            "#ec4899", // pink
            "#6366f1", // indigo
            "#14b8a6"  // teal
        };
        
        long count = collaboratorRepository.countByDiagramIdAndIsActiveTrue(diagramId);
        return colors[(int) (count % colors.length)];
    }
    
    private void broadcastPresence(String diagramId, String type, CollaboratorResponse collaborator) {
        PresenceMessage message = PresenceMessage.builder()
            .type(type)
            .diagramId(diagramId)
            .collaborator(collaborator)
            .timestamp(Instant.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", message);
    }
}
