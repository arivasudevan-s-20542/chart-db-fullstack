package com.chartdb.dto.request;

import lombok.Data;

@Data
public class CreateCustomTypeRequest {
    private String id;
    private String name;
    private String kind;
    private String schemaName;
    private String values;
    private String attributes;
    private String description;
}
