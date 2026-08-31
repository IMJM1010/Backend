package com.example.be.domain.wearabledevice.dto.request;

import com.example.be.domain.wearabledevice.entity.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

/**
 * 디바이스 상태 갱신 요청. 게이트웨이가 주기적으로 호출한다.
 */
@Schema(description = "디바이스 상태 갱신 요청")
public record DeviceStatusUpdateRequest(

        @Schema(description = "배터리 잔량 (0~100)", example = "78")
        @Min(value = 0, message = "배터리 잔량은 0 이상이어야 합니다.")
        @Max(value = 100, message = "배터리 잔량은 100 이하여야 합니다.")
        Integer batteryLevel,

        @Schema(description = "연결 상태", example = "CONNECTED")
        ConnectionStatus connectionStatus,

        @Schema(description = "동기화 시각. 생략하면 서버 시각을 사용한다")
        LocalDateTime lastSyncedAt
) {
}
