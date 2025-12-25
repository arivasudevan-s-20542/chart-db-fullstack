package com.chartdb.service;

import com.chartdb.dto.request.CreateAreaRequest;
import com.chartdb.dto.response.AreaResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.AreaMapper;
import com.chartdb.model.Diagram;
import com.chartdb.model.DiagramArea;
import com.chartdb.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final AreaMapper areaMapper;
    private final DiagramService diagramService;

    @Transactional(readOnly = true)
    public List<AreaResponse> getAreas(String diagramId, String userId) {
        // Verify user has access to the diagram (uses findDiagramById which is read-only)
        Diagram diagram = diagramService.findDiagramById(diagramId);
        if (!diagramService.canUserView(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to view this diagram");
        }
        
        List<DiagramArea> areas = areaRepository.findByDiagramIdOrderBySortOrder(diagramId);
        return areaMapper.toResponseList(areas);
    }

    @Transactional
    public AreaResponse createArea(String diagramId, String userId, CreateAreaRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramArea area = areaMapper.toEntity(request);
        area.setDiagram(diagram);
        
        if (area.getId() == null) {
            area.setId(java.util.UUID.randomUUID().toString());
        }
        
        // Set sort order and z-index if not provided
        if (area.getSortOrder() == null || area.getSortOrder() == 0) {
            area.setSortOrder(areaRepository.findMaxSortOrder(diagramId) + 1);
        }
        if (area.getZIndex() == null || area.getZIndex() == 0) {
            area.setZIndex(areaRepository.findMaxZIndex(diagramId) + 1);
        }

        area = areaRepository.save(area);
        log.info("Area created: {} in diagram {} by user {}", area.getId(), diagramId, userId);
        
        return areaMapper.toResponse(area);
    }

    @Transactional
    public AreaResponse updateArea(String diagramId, String areaId, String userId, CreateAreaRequest request) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        DiagramArea area = areaRepository.findById(areaId)
            .orElseThrow(() -> new ResourceNotFoundException("Area", "id", areaId));

        areaMapper.updateEntity(request, area);
        area = areaRepository.save(area);
        
        log.info("Area updated: {} in diagram {} by user {}", areaId, diagramId, userId);
        return areaMapper.toResponse(area);
    }

    @Transactional
    public void deleteArea(String diagramId, String areaId, String userId) {
        Diagram diagram = diagramService.findDiagramById(diagramId);
        
        if (!diagramService.canUserEdit(diagram, userId)) {
            throw new com.chartdb.exception.AccessDeniedException("You don't have permission to edit this diagram");
        }

        if (!areaRepository.existsById(areaId)) {
            throw new ResourceNotFoundException("Area", "id", areaId);
        }

        areaRepository.deleteById(areaId);
        log.info("Area deleted: {} from diagram {} by user {}", areaId, diagramId, userId);
    }
}
