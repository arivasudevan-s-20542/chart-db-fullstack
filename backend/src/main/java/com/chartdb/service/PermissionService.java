package com.chartdb.service;

import com.chartdb.dto.request.ShareDiagramRequest;
import com.chartdb.dto.response.PermissionResponse;
import com.chartdb.exception.AccessDeniedException;
import com.chartdb.exception.BadRequestException;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.PermissionMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramPermission;
import com.chartdb.model.User;
import com.chartdb.model.enums.PermissionLevel;
import com.chartdb.repository.DiagramPermissionRepository;
import com.chartdb.repository.DiagramRepository;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final DiagramPermissionRepository permissionRepository;
    private final DiagramRepository diagramRepository;
    private final UserRepository userRepository;
    private final PermissionMapper permissionMapper;
    
    @Transactional
    public void createOwnerPermission(Diagram diagram, User owner) {
        DiagramPermission permission = DiagramPermission.builder()
            .diagram(diagram)
            .user(owner)
            .permissionLevel(PermissionLevel.OWNER)
            .canEdit(true)
            .canComment(true)
            .canView(true)
            .build();
        permissionRepository.save(permission);
    }
    
    @Transactional
    public PermissionResponse shareDiagram(String diagramId, String userId, ShareDiagramRequest request) {
        Diagram diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "id", diagramId));
        
        if (!diagram.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only the owner can share this diagram");
        }
        
        if (request.getEmail().equalsIgnoreCase(diagram.getOwner().getEmail())) {
            throw new BadRequestException("Cannot share diagram with yourself");
        }
        
        User targetUser = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);
        
        DiagramPermission existingPermission = null;
        if (targetUser != null) {
            existingPermission = permissionRepository.findByDiagramIdAndUserId(diagramId, targetUser.getId())
                .orElse(null);
        } else {
            existingPermission = permissionRepository.findByDiagramIdAndInvitedEmail(diagramId, request.getEmail())
                .orElse(null);
        }
        
        if (existingPermission != null) {
            updatePermissionLevel(existingPermission, request.getPermissionLevel());
            return permissionMapper.toResponse(permissionRepository.save(existingPermission));
        }
        
        DiagramPermission permission = DiagramPermission.builder()
            .diagram(diagram)
            .user(targetUser)
            .invitedEmail(targetUser == null ? request.getEmail() : null)
            .permissionLevel(request.getPermissionLevel())
            .canEdit(request.getPermissionLevel() == PermissionLevel.EDITOR || request.getPermissionLevel() == PermissionLevel.OWNER)
            .canComment(request.getPermissionLevel() != PermissionLevel.VIEWER)
            .canView(true)
            .invitationStatus(targetUser == null ? "PENDING" : "ACCEPTED")
            .invitedAt(Instant.now())
            .acceptedAt(targetUser != null ? Instant.now() : null)
            .build();
        
        permission = permissionRepository.save(permission);
        log.info("Diagram {} shared with {} by user {}", diagramId, request.getEmail(), userId);
        
        return permissionMapper.toResponse(permission);
    }
    
    @Transactional(readOnly = true)
    public List<PermissionResponse> getDiagramPermissions(String diagramId, String userId) {
        Diagram diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "id", diagramId));
        
        if (!diagram.getOwner().getId().equals(userId) && 
            !hasPermission(diagramId, userId, PermissionLevel.VIEWER)) {
            throw new AccessDeniedException("You don't have access to this diagram");
        }
        
        List<DiagramPermission> permissions = permissionRepository.findByDiagramIdWithUser(diagramId);
        return permissionMapper.toResponseList(permissions);
    }
    
    @Transactional
    public void removePermission(String diagramId, String userId, String targetUserId) {
        Diagram diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "id", diagramId));
        
        if (!diagram.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only the owner can remove permissions");
        }
        
        if (targetUserId.equals(diagram.getOwner().getId())) {
            throw new BadRequestException("Cannot remove owner permission");
        }
        
        permissionRepository.deleteByDiagramIdAndUserId(diagramId, targetUserId);
        log.info("Permission removed from diagram {} for user {} by {}", diagramId, targetUserId, userId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasPermission(String diagramId, String userId, PermissionLevel minimumLevel) {
        return permissionRepository.findByDiagramIdAndUserId(diagramId, userId)
            .map(p -> p.getPermissionLevel().ordinal() <= minimumLevel.ordinal())
            .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public PermissionLevel getPermissionLevel(String diagramId, String userId) {
        return permissionRepository.findByDiagramIdAndUserId(diagramId, userId)
            .map(DiagramPermission::getPermissionLevel)
            .orElse(null);
    }
    
    @Transactional(readOnly = true)
    public void checkReadAccess(String diagramId, String userId) {
        Diagram diagram = diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram", "id", diagramId));
        
        if (Boolean.TRUE.equals(diagram.getIsPublic())) return;
        if (diagram.getOwner().getId().equals(userId)) return;
        if (!hasPermission(diagramId, userId, PermissionLevel.VIEWER)) {
            throw new AccessDeniedException("You don't have access to this diagram");
        }
    }
    
    @Transactional(readOnly = true)
    public boolean canEdit(String diagramId, String userId) {
        return permissionRepository.canUserEdit(diagramId, userId);
    }
    
    private void updatePermissionLevel(DiagramPermission permission, PermissionLevel level) {
        permission.setPermissionLevel(level);
        permission.setCanEdit(level == PermissionLevel.EDITOR || level == PermissionLevel.OWNER);
        permission.setCanComment(level != PermissionLevel.VIEWER);
    }
}
