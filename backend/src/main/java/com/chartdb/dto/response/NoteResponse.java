package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class NoteResponse {
    private String id;
    private String diagramId;
    private String content;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private BigDecimal height;
    private String color;
    private String backgroundColor;
    private Integer zIndex;
    private Instant createdAt;
    private Instant updatedAt;
}
