package com.example.be.domain.vitalrecord.dto.response;

import com.example.be.domain.vitalrecord.entity.VitalRecord;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.worker.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "생체 기록 응답")
public record VitalRecordResponse(
        Long vitalRecordId,
        Long deviceId,
        String serialNo,
        Long workerId,
        String workerName,
        Integer heartRate,
        BigDecimal bodyTemp,
        Integer stepCount,
        LocalDateTime measuredAt,
        LocalDateTime createdAt
) {

    public static VitalRecordResponse from(VitalRecord record) {
        WearableDevice device = record.getDevice();
        Worker worker = device.getWorker();

        return new VitalRecordResponse(
                record.getId(),
                device.getId(),
                device.getSerialNo(),
                worker != null ? worker.getId() : null,
                worker != null ? worker.getName() : null,
                record.getHeartRate(),
                record.getBodyTemp(),
                record.getStepCount(),
                record.getMeasuredAt(),
                record.getCreatedAt()
        );
    }
}
