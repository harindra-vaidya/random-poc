package com.sensor.weathermetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {

    private String message;
    private String sensorId;
    private Instant timestamp;
    private Integer metricsReceived;
}