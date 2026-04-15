package com.sensor.weathermetrics.controller;

import com.sensor.weathermetrics.dto.AggregationResponse;
import com.sensor.weathermetrics.service.SensorAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@Slf4j
public class SensorAggregationController {

    private final SensorAggregationService aggregationService;

    @GetMapping("/{sensorId}/metrics/{metricName}/aggregations")
    public ResponseEntity<AggregationResponse> getAggregations(
            @PathVariable String sensorId,
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) java.util.List<String> statistics) {

        log.info("Received aggregation request for sensor: {}, metric: {}, statistics: {}",
                 sensorId, metricName, statistics);

        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, statistics);

        return ResponseEntity.ok(response);
    }
}