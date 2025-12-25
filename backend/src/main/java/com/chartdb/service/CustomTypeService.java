package com.chartdb.service;

import com.chartdb.dto.request.CreateCustomTypeRequest;
import com.chartdb.dto.response.CustomTypeResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.CustomTypeMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramCustomType;
import com.chartdb.repository.CustomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomTypeService {

    private final CustomTypeRepository customTypeRepository;
    private final CustomTypeMapper customTypeMapper;
    private final DiagramService diagramService;

    @Transactional(readOnly = true)
    public List<CustomTypeResponse> getCustomTypes(String diagramId, String userId) {
        // Verify user has access to the diagram (uses findDiagramById which is read-only)
        Diagram diagram = diagramService.findDiagramById(diagramId);
        if (!diagramService.canUserView(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<DiagramCustomType> customTypes = customTypeRepository.findByDiagramId(diagramId);
        return customTypeMapper.toResponseList(customTypes);
    }

    @Transactional
    public CustomTypeResponse createCustomType(String diagramId, String userId, CreateCustomTypeRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramCustomType customType = customTypeMapper.toEntity(request);
        customType.setDiagram(diagram);
        
        if (customType.getId() == null) {
            customType.setId(java.util.UUID.randomUUID().toString());
        }

        customType = customTypeRepository.save(customType);
        log.info("Custom type created: {} in diagram {} by user {}", customType.getId(), diagramId, userId);
        
        return customTypeMapper.toResponse(customType);
    }

    @Transactional
    public CustomTypeResponse updateCustomType(String diagramId, String customTypeId, String userId, CreateCustomTypeRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramCustomType customType = customTypeRepository.findById(customTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("CustomType", "id", customTypeId));

        customTypeMapper.updateEntity(request, customType);
        customType = customTypeRepository.save(customType);
        
        log.info("Custom type updated: {} in diagram {} by user {}", customTypeId, diagramId, userId);
        return customTypeMapper.toResponse(customType);
    }

    @Transactional
    public void deleteCustomType(String diagramId, String customTypeId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        if (!customTypeRepository.existsById(customTypeId)) {
            throw new ResourceNotFoundException("CustomType", "id", customTypeId);
        }

        customTypeRepository.deleteById(customTypeId);
        log.info("Custom type deleted: {} from diagram {} by user {}", customTypeId, diagramId, userId);
    }
}
