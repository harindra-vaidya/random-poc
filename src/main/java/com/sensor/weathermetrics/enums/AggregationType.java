package com.sensor.weathermetrics.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AggregationType {
    AVG("AVG"),
    MIN("MIN"),
    MAX("MAX"),
    COUNT("COUNT");

    private final String value;

    AggregationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AggregationType fromString(String value) {
        for (AggregationType type : AggregationType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown statistic type: " + value);
    }

    public static List<AggregationType> getAllTypes() {
        return Arrays.asList(values());
    }

    public static List<String> fromStringList(List<String> values) {
        return values.stream()
            .map(AggregationType::fromString)
            .map(AggregationType::getValue)
            .collect(Collectors.toList());
    }
}