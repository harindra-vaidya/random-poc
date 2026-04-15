package com.sensor.weathermetrics.service;

import com.sensor.weathermetrics.domain.Metric;
import com.sensor.weathermetrics.domain.SensorReading;
import com.sensor.weathermetrics.dto.IngestionResponse;
import com.sensor.weathermetrics.dto.MetricReadingRequest;
import com.sensor.weathermetrics.repository.MetricRepository;
import com.sensor.weathermetrics.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensorIngestionService Unit Tests")
class SensorIngestionServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private MetricRepository metricRepository;

    @InjectMocks
    private SensorIngestionService ingestionService;

    @Captor
    private ArgumentCaptor<List<SensorReading>> sensorReadingListCaptor;

    private Metric temperatureMetric;
    private Metric humidityMetric;
    private Metric windSpeedMetric;

    @BeforeEach
    void setUp() {
        temperatureMetric = Metric.builder()
            .id(1L)
            .name("temperature")
            .unit("celsius")
            .createdAt(Instant.now())
            .build();

        humidityMetric = Metric.builder()
            .id(2L)
            .name("humidity")
            .unit("percentage")
            .createdAt(Instant.now())
            .build();

        windSpeedMetric = Metric.builder()
            .id(3L)
            .name("wind_speed")
            .unit("km/h")
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("Should ingest single metric reading successfully")
    void shouldIngestSingleMetricReadingSuccessfully() {
        // Given
        String sensorId = "sensor-001";
        Instant timestamp = Instant.parse("2026-04-15T10:00:00Z");

        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId(sensorId)
            .timestamp(timestamp)
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(25.5)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("temperature"))
            .thenReturn(Optional.of(temperatureMetric));

        // When
        IngestionResponse response = ingestionService.ingestReading(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSensorId()).isEqualTo(sensorId);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getMetricsReceived()).isEqualTo(1);

        verify(sensorReadingRepository, times(1)).saveAll(anyList());
        verify(metricRepository).findByName("temperature");
    }

    @Test
    @DisplayName("Should ingest multiple metrics with exact values")
    void shouldIngestMultipleMetricsWithExactValues() {
        // Given
        String sensorId = "sensor-002";
        Instant timestamp = Instant.parse("2026-04-15T10:00:00Z");

        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId(sensorId)
            .timestamp(timestamp)
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(25.5)
                    .build(),
                MetricReadingRequest.MetricValue.builder()
                    .metricName("humidity")
                    .value(65.0)
                    .build(),
                MetricReadingRequest.MetricValue.builder()
                    .metricName("wind_speed")
                    .value(12.3)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("temperature"))
            .thenReturn(Optional.of(temperatureMetric));
        when(metricRepository.findByName("humidity"))
            .thenReturn(Optional.of(humidityMetric));
        when(metricRepository.findByName("wind_speed"))
            .thenReturn(Optional.of(windSpeedMetric));

        // When
        IngestionResponse response = ingestionService.ingestReading(request);

        // Then
        assertThat(response.getMetricsReceived()).isEqualTo(3);

        verify(sensorReadingRepository).saveAll(sensorReadingListCaptor.capture());

        List<SensorReading> savedReadings = sensorReadingListCaptor.getValue();
        assertThat(savedReadings).hasSize(3);

        // Verify temperature reading
        SensorReading tempReading = savedReadings.stream()
            .filter(r -> r.getMetric().getName().equals("temperature"))
            .findFirst()
            .orElseThrow();
        assertThat(tempReading.getValue()).isEqualByComparingTo(new BigDecimal("25.5"));
        assertThat(tempReading.getSensorId()).isEqualTo(sensorId);
        assertThat(tempReading.getTimestamp()).isEqualTo(timestamp);

        // Verify humidity reading
        SensorReading humidityReading = savedReadings.stream()
            .filter(r -> r.getMetric().getName().equals("humidity"))
            .findFirst()
            .orElseThrow();
        assertThat(humidityReading.getValue()).isEqualByComparingTo(new BigDecimal("65.0"));

        // Verify wind_speed reading
        SensorReading windReading = savedReadings.stream()
            .filter(r -> r.getMetric().getName().equals("wind_speed"))
            .findFirst()
            .orElseThrow();
        assertThat(windReading.getValue()).isEqualByComparingTo(new BigDecimal("12.3"));
    }

    @Test
    @DisplayName("Should throw exception for unknown metric")
    void shouldThrowExceptionForUnknownMetric() {
        // Given
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-003")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("unknown_metric")
                    .value(100.0)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("unknown_metric"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> ingestionService.ingestReading(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown metric: unknown_metric");

        verify(sensorReadingRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle precise decimal values")
    void shouldHandlePreciseDecimalValues() {
        // Given - Test with precise decimal values
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-004")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(25.1234)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("temperature"))
            .thenReturn(Optional.of(temperatureMetric));

        // When
        ingestionService.ingestReading(request);

        // Then
        verify(sensorReadingRepository).saveAll(sensorReadingListCaptor.capture());

        SensorReading savedReading = sensorReadingListCaptor.getValue().get(0);
        assertThat(savedReading.getValue()).isEqualByComparingTo(new BigDecimal("25.1234"));
    }

    @Test
    @DisplayName("Should handle negative values")
    void shouldHandleNegativeValues() {
        // Given
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-005")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(-15.5)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("temperature"))
            .thenReturn(Optional.of(temperatureMetric));

        // When
        ingestionService.ingestReading(request);

        // Then
        verify(sensorReadingRepository).saveAll(sensorReadingListCaptor.capture());

        SensorReading savedReading = sensorReadingListCaptor.getValue().get(0);
        assertThat(savedReading.getValue()).isEqualByComparingTo(new BigDecimal("-15.5"));
    }

    @Test
    @DisplayName("Should handle zero value")
    void shouldHandleZeroValue() {
        // Given
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-006")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("wind_speed")
                    .value(0.0)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("wind_speed"))
            .thenReturn(Optional.of(windSpeedMetric));

        // When
        ingestionService.ingestReading(request);

        // Then
        verify(sensorReadingRepository).saveAll(sensorReadingListCaptor.capture());

        SensorReading savedReading = sensorReadingListCaptor.getValue().get(0);
        assertThat(savedReading.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle large values")
    void shouldHandleLargeValues() {
        // Given
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-008")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(999999.9999)
                    .build()
            ))
            .build();

        when(metricRepository.findByName("temperature"))
            .thenReturn(Optional.of(temperatureMetric));

        // When
        ingestionService.ingestReading(request);

        // Then
        verify(sensorReadingRepository).saveAll(sensorReadingListCaptor.capture());

        SensorReading savedReading = sensorReadingListCaptor.getValue().get(0);
        assertThat(savedReading.getValue()).isEqualByComparingTo(new BigDecimal("999999.9999"));
    }
}