package com.sensor.weathermetrics.integration;

import com.sensor.weathermetrics.dto.AggregationResponse;
import com.sensor.weathermetrics.dto.IngestionResponse;
import com.sensor.weathermetrics.dto.MetricReadingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SensorApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldIngestSensorReadingAndReturnAccepted() {
        // Given
        Instant now = Instant.now();
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-001")
            .timestamp(now)
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("temperature")
                    .value(25.5)
                    .build(),
                MetricReadingRequest.MetricValue.builder()
                    .metricName("humidity")
                    .value(60.0)
                    .build()
            ))
            .build();

        // When
        ResponseEntity<IngestionResponse> response = restTemplate.postForEntity(
            "/api/v1/sensors/ingest",
            request,
            IngestionResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSensorId()).isEqualTo("sensor-001");
        assertThat(response.getBody().getMetricsReceived()).isEqualTo(2);
    }

    @Test
    void shouldReturnAggregationsForSensorReadings() {
        // Given - Ingest multiple readings
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            MetricReadingRequest request = MetricReadingRequest.builder()
                .sensorId("sensor-002")
                .timestamp(baseTime.plus(i, ChronoUnit.MINUTES))
                .metrics(List.of(
                    MetricReadingRequest.MetricValue.builder()
                        .metricName("temperature")
                        .value(20.0 + i)
                        .build()
                ))
                .build();

            restTemplate.postForEntity("/api/v1/sensors/ingest", request, IngestionResponse.class);
        }

        // When - Query aggregations
        String url = String.format(
            "/api/v1/sensors/sensor-002/metrics/temperature/aggregations?startTime=%s&endTime=%s",
            baseTime.toString(),
            baseTime.plus(10, ChronoUnit.MINUTES).toString()
        );

        ResponseEntity<AggregationResponse> response = restTemplate.getForEntity(
            url,
            AggregationResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSensorId()).isEqualTo("sensor-002");
        assertThat(response.getBody().getMetricName()).isEqualTo("temperature");
        assertThat(response.getBody().getAggregations()).hasSize(4); // AVG, MIN, MAX, COUNT

        // Verify aggregation values
        AggregationResponse.AggregatedValue avgValue = response.getBody().getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("AVG"))
            .findFirst()
            .orElseThrow();
        assertThat(avgValue.getValue().doubleValue()).isEqualTo(22.0);

        AggregationResponse.AggregatedValue minValue = response.getBody().getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MIN"))
            .findFirst()
            .orElseThrow();
        assertThat(minValue.getValue().doubleValue()).isEqualTo(20.0);

        AggregationResponse.AggregatedValue maxValue = response.getBody().getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("MAX"))
            .findFirst()
            .orElseThrow();
        assertThat(maxValue.getValue().doubleValue()).isEqualTo(24.0);

        AggregationResponse.AggregatedValue countValue = response.getBody().getAggregations().stream()
            .filter(agg -> agg.getAggregationType().equals("COUNT"))
            .findFirst()
            .orElseThrow();
        assertThat(countValue.getValue().longValue()).isEqualTo(5L);
    }

    @Test
    void shouldReturnBadRequestForInvalidMetricName() {
        // Given
        MetricReadingRequest request = MetricReadingRequest.builder()
            .sensorId("sensor-003")
            .timestamp(Instant.now())
            .metrics(List.of(
                MetricReadingRequest.MetricValue.builder()
                    .metricName("invalid_metric")
                    .value(100.0)
                    .build()
            ))
            .build();

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/sensors/ingest",
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}