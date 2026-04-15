package com.sensor.weathermetrics.repository;

import com.sensor.weathermetrics.domain.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    Optional<Metric> findByName(String name);

    boolean existsByName(String name);
}