package com.chartdb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AreaResponse {
    private String id;
    private String diagramId;
    private String name;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private BigDecimal height;
    private String color;
    private String backgroundColor;
    private String borderColor;
    private BigDecimal opacity;
    private Integer zIndex;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
