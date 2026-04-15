# Weather Metrics API

REST API for weather sensor metrics ingestion and aggregation.

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- PostgreSQL 16
- Spring Data JPA
- Flyway (database migrations)
- Lombok
- Docker Compose

## Architecture

Clean layered architecture with repository pattern:
- **Controller Layer**: REST endpoints for ingestion and aggregation
- **Service Layer**: Business logic
- **Repository Layer**: Data access abstraction (interface-based for future storage swaps)
- **Domain Layer**: JPA entities with normalized metric design

### Key Design Decisions

1. **Normalized Metrics Schema**: Separate `metrics` and `sensor_readings` tables for flexibility
2. **Repository Interface**: `SensorReadingRepository` as seam for future Elasticsearch implementation
3. **202 Accepted**: Ingestion endpoint returns 202 for async processing semantics

## Quick Start

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

### 2. Run Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. Run Tests

```bash
./mvnw test
```

## API Endpoints

### Ingest Sensor Reading

```bash
POST /api/v1/sensors/ingest
Content-Type: application/json

{
  "sensorId": "sensor-001",
  "timestamp": "2026-04-14T10:30:00Z",
  "metrics": [
    {
      "metricName": "temperature",
      "value": 25.5
    },
    {
      "metricName": "humidity",
      "value": 60.0
    },
    {
      "metricName": "wind_speed",
      "value": 12.3
    }
  ]
}
```

**Response:** `202 Accepted`

```json
{
  "message": "Reading accepted for processing",
  "sensorId": "sensor-001",
  "timestamp": "2026-04-14T10:30:00Z",
  "metricsReceived": 3
}
```

### Get Aggregations

```bash
GET /api/v1/sensors/{sensorId}/metrics/{metricName}/aggregations?startTime={ISO-8601}&endTime={ISO-8601}
```

**Example:**

```bash
GET /api/v1/sensors/sensor-001/metrics/temperature/aggregations?startTime=2026-04-14T00:00:00Z&endTime=2026-04-14T23:59:59Z
```

**Response:** `200 OK`

```json
{
  "sensorId": "sensor-001",
  "metricName": "temperature",
  "startTime": "2026-04-14T00:00:00Z",
  "endTime": "2026-04-14T23:59:59Z",
  "aggregations": [
    {
      "aggregationType": "AVG",
      "value": 22.5
    },
    {
      "aggregationType": "MIN",
      "value": 18.0
    },
    {
      "aggregationType": "MAX",
      "value": 28.0
    },
    {
      "aggregationType": "COUNT",
      "value": 120
    }
  ]
}
```

## Available Metrics

- `temperature` (celsius)
- `humidity` (percentage)
- `wind_speed` (km/h)

## Database Schema

### metrics
```sql
id          BIGSERIAL PRIMARY KEY
name        VARCHAR(50) UNIQUE NOT NULL
unit        VARCHAR(50) NOT NULL
created_at  TIMESTAMP NOT NULL
```

### sensor_readings
```sql
id          BIGSERIAL PRIMARY KEY
sensor_id   VARCHAR(100) NOT NULL
metric_id   BIGINT NOT NULL (FK to metrics)
value       NUMERIC(10, 4) NOT NULL
timestamp   TIMESTAMP NOT NULL
created_at  TIMESTAMP NOT NULL
```

**Indexes:**
- `idx_sensor_id_timestamp` on (sensor_id, timestamp)
- `idx_metric_id_timestamp` on (metric_id, timestamp)
- `idx_timestamp` on (timestamp)

## Future Evolution

The repository interface design enables seamless storage evolution:
- **Write path**: Insert Kafka between API and storage for async processing
- **Read path**: Swap PostgreSQL with Elasticsearch for optimized time-series queries
- **Contract**: API endpoints and response shapes remain unchanged