package com.example.be.domain.wearabledevice.dto.request;

import com.example.be.domain.wearabledevice.entity.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "디바이스 등록 요청")
public record WearableDeviceCreateRequest(

        @Schema(description = "착용시킬 작업자 식별자. 생략하면 미배정 상태로 등록", example = "1")
        Long workerId,

        @Schema(description = "시리얼 번호. 중복될 수 없다", example = "BAND-0001")
        @NotBlank(message = "시리얼 번호는 필수입니다.")
        @Size(max = 50, message = "시리얼 번호는 50자 이하여야 합니다.")
        String serialNo,

        @Schema(description = "디바이스 종류", example = "BAND")
        @NotNull(message = "디바이스 종류는 필수입니다.")
        DeviceType deviceType
) {
}
