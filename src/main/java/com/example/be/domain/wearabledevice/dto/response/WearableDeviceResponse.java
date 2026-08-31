package com.example.be.domain.wearabledevice.dto.response;

import com.example.be.domain.wearabledevice.entity.ConnectionStatus;
import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.worker.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "디바이스 응답")
public record WearableDeviceResponse(
        Long deviceId,
        Long workerId,
        String workerName,
        String employeeNo,
        String serialNo,
        DeviceType deviceType,
        Integer batteryLevel,
        ConnectionStatus connectionStatus,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WearableDeviceResponse from(WearableDevice device) {
        Worker worker = device.getWorker();

        return new WearableDeviceResponse(
                device.getId(),
                worker != null ? worker.getId() : null,
                worker != null ? worker.getName() : null,
                worker != null ? worker.getEmployeeNo() : null,
                device.getSerialNo(),
                device.getDeviceType(),
                device.getBatteryLevel(),
                device.getConnectionStatus(),
                device.getLastSyncedAt(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
