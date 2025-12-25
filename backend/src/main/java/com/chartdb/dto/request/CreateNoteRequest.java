package com.chartdb.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateNoteRequest {
    private String id;
    private String content;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal width;
    private BigDecimal height;
    private String color;
    private String backgroundColor;
    private Integer zIndex;
}
