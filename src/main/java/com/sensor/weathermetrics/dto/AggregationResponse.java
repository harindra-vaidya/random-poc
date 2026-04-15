package com.sensor.weathermetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationResponse {

    private String sensorId;
    private String metricName;
    private Instant startTime;
    private Instant endTime;
    private List<AggregatedValue> aggregations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregatedValue {
        private String aggregationType; // AVG, MIN, MAX, COUNT
        private BigDecimal value;
    }
}