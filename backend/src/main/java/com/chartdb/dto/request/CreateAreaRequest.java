package com.chartdb.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateAreaRequest {
    private String id;
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
}
