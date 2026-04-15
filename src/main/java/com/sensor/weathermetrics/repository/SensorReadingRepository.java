package com.sensor.weathermetrics.repository;

import com.sensor.weathermetrics.domain.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for SensorReading.
 *
 * This interface serves as the seam for storage abstraction.
 * Current implementation: Postgres via Spring Data JPA
 * Future evolution: Elasticsearch implementation for read queries
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findBySensorIdAndTimestampBetween(
        String sensorId,
        Instant startTime,
        Instant endTime
    );

    @Query("""
        SELECT sr FROM SensorReading sr
        JOIN FETCH sr.metric
        WHERE sr.sensorId = :sensorId
        AND sr.metric.name = :metricName
        AND sr.timestamp BETWEEN :startTime AND :endTime
        ORDER BY sr.timestamp ASC
        """)
    List<SensorReading> findBySensorIdAndMetricNameAndTimestampBetween(
        @Param("sensorId") String sensorId,
        @Param("metricName") String metricName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT AVG(sr.value) FROM SensorReading sr
        WHERE sr.sensorId = :sensorId
        AND sr.metric.name = :metricName
        AND sr.timestamp BETWEEN :startTime AND :endTime
        """)
    Double calculateAverage(
        @Param("sensorId") String sensorId,
        @Param("metricName") String metricName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT MIN(sr.value) FROM SensorReading sr
        WHERE sr.sensorId = :sensorId
        AND sr.metric.name = :metricName
        AND sr.timestamp BETWEEN :startTime AND :endTime
        """)
    Double calculateMin(
        @Param("sensorId") String sensorId,
        @Param("metricName") String metricName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT MAX(sr.value) FROM SensorReading sr
        WHERE sr.sensorId = :sensorId
        AND sr.metric.name = :metricName
        AND sr.timestamp BETWEEN :startTime AND :endTime
        """)
    Double calculateMax(
        @Param("sensorId") String sensorId,
        @Param("metricName") String metricName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT COUNT(sr) FROM SensorReading sr
        WHERE sr.sensorId = :sensorId
        AND sr.metric.name = :metricName
        AND sr.timestamp BETWEEN :startTime AND :endTime
        """)
    Long calculateCount(
        @Param("sensorId") String sensorId,
        @Param("metricName") String metricName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}