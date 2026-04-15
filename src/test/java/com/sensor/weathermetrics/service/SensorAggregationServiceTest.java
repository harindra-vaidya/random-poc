package com.sensor.weathermetrics.service;

import com.sensor.weathermetrics.dto.AggregationResponse;
import com.sensor.weathermetrics.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensorAggregationService Unit Tests")
class SensorAggregationServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @InjectMocks
    private SensorAggregationService aggregationService;

    private String sensorId;
    private String metricName;
    private Instant startTime;
    private Instant endTime;

    @BeforeEach
    void setUp() {
        sensorId = "sensor-001";
        metricName = "temperature";
        startTime = Instant.parse("2026-04-15T10:00:00Z");
        endTime = Instant.parse("2026-04-15T11:00:00Z");
    }

    @Test
    @DisplayName("Should calculate all statistics with exact values")
    void shouldCalculateAllStatisticsWithExactValues() {
        // Given - Test data: 20.0, 22.5, 25.0, 23.0, 24.5
        // Expected: AVG = 23.0, MIN = 20.0, MAX = 25.0, COUNT = 5
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(23.0);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(20.0);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.0);
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(5L);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSensorId()).isEqualTo(sensorId);
        assertThat(response.getMetricName()).isEqualTo(metricName);
        assertThat(response.getStartTime()).isEqualTo(startTime);
        assertThat(response.getEndTime()).isEqualTo(endTime);
        assertThat(response.getAggregations()).hasSize(4);

        // Verify exact AVG value
        AggregationResponse.AggregatedValue avg = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("AVG"))
            .findFirst()
            .orElseThrow();
        assertThat(avg.getValue()).isEqualByComparingTo(new BigDecimal("23.0"));

        // Verify exact MIN value
        AggregationResponse.AggregatedValue min = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MIN"))
            .findFirst()
            .orElseThrow();
        assertThat(min.getValue()).isEqualByComparingTo(new BigDecimal("20.0"));

        // Verify exact MAX value
        AggregationResponse.AggregatedValue max = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MAX"))
            .findFirst()
            .orElseThrow();
        assertThat(max.getValue()).isEqualByComparingTo(new BigDecimal("25.0"));

        // Verify exact COUNT value
        AggregationResponse.AggregatedValue count = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("COUNT"))
            .findFirst()
            .orElseThrow();
        assertThat(count.getValue()).isEqualByComparingTo(new BigDecimal("5"));

        // Verify repository was called
        verify(sensorReadingRepository).calculateAverage(sensorId, metricName, startTime, endTime);
        verify(sensorReadingRepository).calculateMin(sensorId, metricName, startTime, endTime);
        verify(sensorReadingRepository).calculateMax(sensorId, metricName, startTime, endTime);
        verify(sensorReadingRepository).calculateCount(sensorId, metricName, startTime, endTime);
    }

    @Test
    @DisplayName("Should calculate only AVG when requested")
    void shouldCalculateOnlyAvgWhenRequested() {
        // Given
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(23.0);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("AVG"));

        // Then
        assertThat(response.getAggregations()).hasSize(1);
        assertThat(response.getAggregations().get(0).getAggregationType()).isEqualTo("AVG");
        assertThat(response.getAggregations().get(0).getValue()).isEqualByComparingTo(new BigDecimal("23.0"));

        verify(sensorReadingRepository).calculateAverage(sensorId, metricName, startTime, endTime);
    }

    @Test
    @DisplayName("Should calculate only MIN and MAX when requested")
    void shouldCalculateOnlyMinAndMaxWhenRequested() {
        // Given
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(20.0);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.0);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("MIN", "MAX"));

        // Then
        assertThat(response.getAggregations()).hasSize(2);

        AggregationResponse.AggregatedValue min = response.getAggregations().get(0);
        assertThat(min.getAggregationType()).isEqualTo("MIN");
        assertThat(min.getValue()).isEqualByComparingTo(new BigDecimal("20.0"));

        AggregationResponse.AggregatedValue max = response.getAggregations().get(1);
        assertThat(max.getAggregationType()).isEqualTo("MAX");
        assertThat(max.getValue()).isEqualByComparingTo(new BigDecimal("25.0"));

        verify(sensorReadingRepository).calculateMin(sensorId, metricName, startTime, endTime);
        verify(sensorReadingRepository).calculateMax(sensorId, metricName, startTime, endTime);
    }

    @Test
    @DisplayName("Should calculate only COUNT when requested")
    void shouldCalculateOnlyCountWhenRequested() {
        // Given
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(5L);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("COUNT"));

        // Then
        assertThat(response.getAggregations()).hasSize(1);
        assertThat(response.getAggregations().get(0).getAggregationType()).isEqualTo("COUNT");
        assertThat(response.getAggregations().get(0).getValue()).isEqualByComparingTo(new BigDecimal("5"));

        verify(sensorReadingRepository).calculateCount(sensorId, metricName, startTime, endTime);
    }

    @Test
    @DisplayName("Should handle null values from repository")
    void shouldHandleNullValuesFromRepository() {
        // Given - Repository returns null for all statistics (no data)
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(null);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(null);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(null);
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(null);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, null);

        // Then - Should return empty aggregations list
        assertThat(response.getAggregations()).isEmpty();
    }

    @Test
    @DisplayName("Should handle decimal precision correctly")
    void shouldHandleDecimalPrecisionCorrectly() {
        // Given - Test precise decimal values
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(22.3333333333);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(19.5);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.125);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("AVG", "MIN", "MAX"));

        // Then - Verify exact decimal values
        AggregationResponse.AggregatedValue avg = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("AVG"))
            .findFirst()
            .orElseThrow();
        assertThat(avg.getValue().doubleValue()).isEqualTo(22.3333333333);

        AggregationResponse.AggregatedValue min = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MIN"))
            .findFirst()
            .orElseThrow();
        assertThat(min.getValue().doubleValue()).isEqualTo(19.5);

        AggregationResponse.AggregatedValue max = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MAX"))
            .findFirst()
            .orElseThrow();
        assertThat(max.getValue().doubleValue()).isEqualTo(25.125);
    }

    @Test
    @DisplayName("Should handle case-insensitive statistic names")
    void shouldHandleCaseInsensitiveStatisticNames() {
        // Given
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(23.0);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(20.0);

        // When - Using lowercase statistic names
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("avg", "min"));

        // Then
        assertThat(response.getAggregations()).hasSize(2);
        assertThat(response.getAggregations().get(0).getAggregationType()).isEqualTo("AVG");
        assertThat(response.getAggregations().get(1).getAggregationType()).isEqualTo("MIN");
    }

    @Test
    @DisplayName("Should throw exception for unknown statistic type")
    void shouldThrowExceptionForUnknownStatisticType() {
        // When/Then
        assertThatThrownBy(() ->
            aggregationService.getAggregations(
                sensorId, metricName, startTime, endTime, List.of("INVALID_STAT")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown statistic type: INVALID_STAT");
    }

    @Test
    @DisplayName("Should handle empty statistics list by returning all statistics")
    void shouldHandleEmptyStatisticsListByReturningAll() {
        // Given
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(23.0);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(20.0);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.0);
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(5L);

        // When - Empty list should return all statistics
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of());

        // Then
        assertThat(response.getAggregations()).hasSize(4);
    }

    @Test
    @DisplayName("Should calculate statistics for single data point")
    void shouldCalculateStatisticsForSingleDataPoint() {
        // Given - Only one reading with value 25.5
        when(sensorReadingRepository.calculateAverage(sensorId, metricName, startTime, endTime))
            .thenReturn(25.5);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(25.5);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.5);
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(1L);

        // When
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, null);

        // Then - All statistics should be equal to the single value
        assertThat(response.getAggregations()).hasSize(4);

        AggregationResponse.AggregatedValue avg = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("AVG"))
            .findFirst()
            .orElseThrow();
        assertThat(avg.getValue()).isEqualByComparingTo(new BigDecimal("25.5"));

        AggregationResponse.AggregatedValue min = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MIN"))
            .findFirst()
            .orElseThrow();
        assertThat(min.getValue()).isEqualByComparingTo(new BigDecimal("25.5"));

        AggregationResponse.AggregatedValue max = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MAX"))
            .findFirst()
            .orElseThrow();
        assertThat(max.getValue()).isEqualByComparingTo(new BigDecimal("25.5"));

        AggregationResponse.AggregatedValue count = response.getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("COUNT"))
            .findFirst()
            .orElseThrow();
        assertThat(count.getValue()).isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    @DisplayName("Should preserve order of requested statistics")
    void shouldPreserveOrderOfRequestedStatistics() {
        // Given
        when(sensorReadingRepository.calculateCount(sensorId, metricName, startTime, endTime))
            .thenReturn(5L);
        when(sensorReadingRepository.calculateMax(sensorId, metricName, startTime, endTime))
            .thenReturn(25.0);
        when(sensorReadingRepository.calculateMin(sensorId, metricName, startTime, endTime))
            .thenReturn(20.0);

        // When - Request in specific order: COUNT, MAX, MIN
        AggregationResponse response = aggregationService.getAggregations(
            sensorId, metricName, startTime, endTime, List.of("COUNT", "MAX", "MIN"));

        // Then - Should preserve order
        assertThat(response.getAggregations()).hasSize(3);
        assertThat(response.getAggregations().get(0).getAggregationType()).isEqualTo("COUNT");
        assertThat(response.getAggregations().get(1).getAggregationType()).isEqualTo("MAX");
        assertThat(response.getAggregations().get(2).getAggregationType()).isEqualTo("MIN");
    }
}
