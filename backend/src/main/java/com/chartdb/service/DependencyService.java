package com.chartdb.service;

import com.chartdb.dto.request.CreateDependencyRequest;
import com.chartdb.dto.response.DependencyResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.DependencyMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramDependency;
import com.chartdb.repository.DependencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyService {

    private final DependencyRepository dependencyRepository;
    private final DependencyMapper dependencyMapper;
    private final DiagramService diagramService;

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependencies(String diagramId, String userId) {
        // Verify user has access to the diagram (uses findDiagramById which is read-only)
        Diagram diagram = diagramService.findDiagramById(diagramId);
        if (!diagramService.canUserView(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<DiagramDependency> dependencies = dependencyRepository.findByDiagramId(diagramId);
        return dependencyMapper.toResponseList(dependencies);
    }

    @Transactional
    public DependencyResponse createDependency(String diagramId, String userId, CreateDependencyRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramDependency dependency = dependencyMapper.toEntity(request);
        dependency.setDiagram(diagram);
        
        if (dependency.getId() == null) {
            dependency.setId(java.util.UUID.randomUUID().toString());
        }

        dependency = dependencyRepository.save(dependency);
        log.info("Dependency created: {} in diagram {} by user {}", dependency.getId(), diagramId, userId);
        
        return dependencyMapper.toResponse(dependency);
    }

    @Transactional
    public DependencyResponse updateDependency(String diagramId, String dependencyId, String userId, CreateDependencyRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramDependency dependency = dependencyRepository.findById(dependencyId)
            .orElseThrow(() -> new ResourceNotFoundException("Dependency", "id", dependencyId));

        dependencyMapper.updateEntity(request, dependency);
        dependency = dependencyRepository.save(dependency);
        
        log.info("Dependency updated: {} in diagram {} by user {}", dependencyId, diagramId, userId);
        return dependencyMapper.toResponse(dependency);
    }

    @Transactional
    public void deleteDependency(String diagramId, String dependencyId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        if (!dependencyRepository.existsById(dependencyId)) {
            throw new ResourceNotFoundException("Dependency", "id", dependencyId);
        }

        dependencyRepository.deleteById(dependencyId);
        log.info("Dependency deleted: {} from diagram {} by user {}", dependencyId, diagramId, userId);
    }
}
