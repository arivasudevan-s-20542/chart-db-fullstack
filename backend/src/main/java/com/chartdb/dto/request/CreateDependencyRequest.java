package com.chartdb.dto.request;

import lombok.Data;

@Data
public class CreateDependencyRequest {
    private String id;
    private String sourceTableId;
    private String targetTableId;
    private String dependencyType;
    private String description;
}
