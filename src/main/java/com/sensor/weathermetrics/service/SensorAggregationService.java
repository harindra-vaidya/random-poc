package com.sensor.weathermetrics.service;

import com.sensor.weathermetrics.dto.AggregationResponse;
import com.sensor.weathermetrics.enums.AggregationType;
import com.sensor.weathermetrics.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorAggregationService {

    private final SensorReadingRepository sensorReadingRepository;

    @Transactional(readOnly = true)
    public AggregationResponse getAggregations(
            String sensorId,
            String metricName,
            Instant startTime,
            Instant endTime,
            List<String> requestedStatistics) {

        log.debug("Calculating aggregations for sensor: {}, metric: {}, period: {} to {}, statistics: {}",
                  sensorId, metricName, startTime, endTime, requestedStatistics);

        List<AggregationType> statisticTypes = determineStatisticTypes(requestedStatistics);
        List<AggregationResponse.AggregatedValue> aggregations = calculateAggregations(
            sensorId, metricName, startTime, endTime, statisticTypes);

        log.info("Calculated {} aggregations for sensor: {}, metric: {}",
                 aggregations.size(), sensorId, metricName);

        return AggregationResponse.builder()
            .sensorId(sensorId)
            .metricName(metricName)
            .startTime(startTime)
            .endTime(endTime)
            .aggregations(aggregations)
            .build();
    }

    private List<AggregationType> determineStatisticTypes(List<String> requestedStatistics) {
        if (requestedStatistics == null || requestedStatistics.isEmpty()) {
            return AggregationType.getAllTypes();
        }
        return requestedStatistics.stream()
            .map(AggregationType::fromString)
            .collect(Collectors.toList());
    }

    private List<AggregationResponse.AggregatedValue> calculateAggregations(
            String sensorId,
            String metricName,
            Instant startTime,
            Instant endTime,
            List<AggregationType> statisticTypes) {

        List<AggregationResponse.AggregatedValue> aggregations = new ArrayList<>();

        for (AggregationType type : statisticTypes) {
            AggregationResponse.AggregatedValue aggregation = calculateSingleAggregation(
                sensorId, metricName, startTime, endTime, type);

            if (aggregation != null) {
                aggregations.add(aggregation);
            }
        }

        return aggregations;
    }

    private AggregationResponse.AggregatedValue calculateSingleAggregation(
            String sensorId,
            String metricName,
            Instant startTime,
            Instant endTime,
            AggregationType type) {

        return switch (type) {
            case AVG -> calculateAverage(sensorId, metricName, startTime, endTime);
            case MIN -> calculateMin(sensorId, metricName, startTime, endTime);
            case MAX -> calculateMax(sensorId, metricName, startTime, endTime);
            case COUNT -> calculateCount(sensorId, metricName, startTime, endTime);
            default -> {
                log.warn("Unknown statistic type requested: {}", type);
                throw new IllegalArgumentException("Unknown statistic type: " + type);
            }
        };
    }

    private AggregationResponse.AggregatedValue calculateAverage(
            String sensorId, String metricName, Instant startTime, Instant endTime) {
        Double avg = sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime);
        return avg != null ? createAggregatedValue(AggregationType.AVG, avg) : null;
    }

    private AggregationResponse.AggregatedValue calculateMin(
            String sensorId, String metricName, Instant startTime, Instant endTime) {
        Double min = sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime);
        return min != null ? createAggregatedValue(AggregationType.MIN, min) : null;
    }

    private AggregationResponse.AggregatedValue calculateMax(
            String sensorId, String metricName, Instant startTime, Instant endTime) {
        Double max = sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime);
        return max != null ? createAggregatedValue(AggregationType.MAX, max) : null;
    }

    private AggregationResponse.AggregatedValue calculateCount(
            String sensorId, String metricName, Instant startTime, Instant endTime) {
        Long count = sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime);
        return count != null ? createAggregatedValue(AggregationType.COUNT, count.doubleValue()) : null;
    }

    private AggregationResponse.AggregatedValue createAggregatedValue(AggregationType type, Double value) {
        return AggregationResponse.AggregatedValue.builder()
            .aggregationType(type.getValue())
            .value(BigDecimal.valueOf(value))
            .build();
    }
}