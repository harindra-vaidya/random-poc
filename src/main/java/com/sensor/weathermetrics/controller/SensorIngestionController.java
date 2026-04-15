package com.sensor.weathermetrics.controller;

import com.sensor.weathermetrics.dto.IngestionResponse;
import com.sensor.weathermetrics.dto.MetricReadingRequest;
import com.sensor.weathermetrics.service.SensorIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@Slf4j
public class SensorIngestionController {

    private final SensorIngestionService ingestionService;

    @PostMapping("/ingest")
    public ResponseEntity<IngestionResponse> ingestReading(
            @Valid @RequestBody MetricReadingRequest request) {

        log.info("Received ingestion request for sensor: {}", request.getSensorId());

        IngestionResponse response = ingestionService.ingestReading(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}