package com.sensor.weathermetrics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_sensor_id_timestamp", columnList = "sensor_id, timestamp"),
    @Index(name = "idx_metric_id_timestamp", columnList = "metric_id, timestamp"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reading_metric"))
    private Metric metric;

    @Column(name = "metric_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}