package com.example.be.domain.worker.dto.response;

import com.example.be.domain.worker.entity.SafetyStatus;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.zone.entity.Zone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 작업자 응답.
 *
 * <p>대시보드가 "어느 공정 어느 구역의 누가 위험한가"를 한 번에 보여줘야 하므로
 * 구역·공정 정보를 함께 내려준다. 구역 미배정이면 관련 필드는 모두 null 이다.
 */
@Schema(description = "작업자 응답")
public record WorkerResponse(
        Long workerId,
        Long zoneId,
        String zoneCode,
        String zoneName,
        Long processId,
        String processName,
        String employeeNo,
        String name,
        SafetyStatus safetyStatus,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WorkerResponse from(Worker worker) {
        Zone zone = worker.getZone();

        return new WorkerResponse(
                worker.getId(),
                zone != null ? zone.getId() : null,
                zone != null ? zone.getZoneCode() : null,
                zone != null ? zone.getName() : null,
                zone != null ? zone.getProcess().getId() : null,
                zone != null ? zone.getProcess().getName() : null,
                worker.getEmployeeNo(),
                worker.getName(),
                worker.getSafetyStatus(),
                worker.isActive(),
                worker.getCreatedAt(),
                worker.getUpdatedAt()
        );
    }
}
