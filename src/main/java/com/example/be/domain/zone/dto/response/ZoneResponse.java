package com.example.be.domain.zone.dto.response;

import com.example.be.domain.zone.entity.Zone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "구역 응답")
public record ZoneResponse(
        Long zoneId,
        Long processId,
        String processName,
        String zoneCode,
        String name,
        BigDecimal mapX,
        BigDecimal mapY,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(
                zone.getId(),
                zone.getProcess().getId(),
                zone.getProcess().getName(),
                zone.getZoneCode(),
                zone.getName(),
                zone.getMapX(),
                zone.getMapY(),
                zone.getCreatedAt(),
                zone.getUpdatedAt()
        );
    }
}
