package com.example.be.domain.envsensor.dto.response;

import com.example.be.domain.envsensor.entity.EnvSensor;
import com.example.be.domain.envsensor.entity.SensorType;
import com.example.be.domain.zone.entity.Zone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "환경 센서 응답")
public record EnvSensorResponse(
        Long sensorId,
        Long zoneId,
        String zoneCode,
        String zoneName,
        SensorType sensorType,
        String unit,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EnvSensorResponse from(EnvSensor sensor) {
        Zone zone = sensor.getZone();
        return new EnvSensorResponse(
                sensor.getId(),
                zone.getId(),
                zone.getZoneCode(),
                zone.getName(),
                sensor.getSensorType(),
                sensor.getUnit(),
                sensor.getThresholdMin(),
                sensor.getThresholdMax(),
                sensor.getCreatedAt(),
                sensor.getUpdatedAt()
        );
    }
}
