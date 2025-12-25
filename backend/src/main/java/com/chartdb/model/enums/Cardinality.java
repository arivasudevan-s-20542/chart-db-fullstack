package com.chartdb.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Cardinality {
    ONE("one"),
    MANY("many");
    
    private final String value;
    
    Cardinality(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static Cardinality fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Cardinality cardinality : Cardinality.values()) {
            if (cardinality.value.equalsIgnoreCase(value) || cardinality.name().equalsIgnoreCase(value)) {
                return cardinality;
            }
        }
        throw new IllegalArgumentException("Unknown cardinality: " + value);
    }
}
