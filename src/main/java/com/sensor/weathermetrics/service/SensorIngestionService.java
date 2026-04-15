package com.sensor.weathermetrics.service;

import com.sensor.weathermetrics.domain.Metric;
import com.sensor.weathermetrics.domain.SensorReading;
import com.sensor.weathermetrics.dto.IngestionResponse;
import com.sensor.weathermetrics.dto.MetricReadingRequest;
import com.sensor.weathermetrics.repository.MetricRepository;
import com.sensor.weathermetrics.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorIngestionService {

    private final SensorReadingRepository sensorReadingRepository;
    private final MetricRepository metricRepository;

    @Transactional
    public IngestionResponse ingestReading(MetricReadingRequest request) {
        log.debug("Ingesting reading for sensor: {} at timestamp: {}",
                  request.getSensorId(), request.getTimestamp());

        List<SensorReading> readings = new ArrayList<>();

        for (MetricReadingRequest.MetricValue metricValue : request.getMetrics()) {
            Metric metric = metricRepository.findByName(metricValue.getMetricName())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown metric: " + metricValue.getMetricName()));

            SensorReading reading = SensorReading.builder()
                .sensorId(request.getSensorId())
                .metric(metric)
                .value(BigDecimal.valueOf(metricValue.getValue()))
                .timestamp(request.getTimestamp())
                .build();

            readings.add(reading);
        }

        sensorReadingRepository.saveAll(readings);

        log.info("Successfully ingested {} metrics for sensor: {}",
                 readings.size(), request.getSensorId());

        return IngestionResponse.builder()
            .message("Reading accepted for processing")
            .sensorId(request.getSensorId())
            .timestamp(request.getTimestamp())
            .metricsReceived(readings.size())
            .build();
    }
}