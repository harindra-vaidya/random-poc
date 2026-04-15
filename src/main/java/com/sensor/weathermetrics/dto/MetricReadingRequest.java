package com.sensor.weathermetrics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricReadingRequest {

    @NotBlank(message = "Sensor ID is required")
    private String sensorId;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @NotEmpty(message = "At least one metric reading is required")
    @Valid
    private List<MetricValue> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricValue {

        @NotBlank(message = "Metric name is required")
        private String metricName;

        @NotNull(message = "Value is required")
        private Double value;
    }
}