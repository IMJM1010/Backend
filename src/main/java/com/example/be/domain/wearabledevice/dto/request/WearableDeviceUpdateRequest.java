package com.example.be.domain.wearabledevice.dto.request;

import com.example.be.domain.wearabledevice.entity.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 디바이스 정보 수정.
 *
 * <p>{@code workerId} 는 명시적으로 보낸 경우에만 반영한다. 필드를 아예 생략하는 것과
 * null 을 보내는 것을 구분해야 "배정 해제"를 표현할 수 있어 {@code unassign} 플래그를 둔다.
 */
@Schema(description = "디바이스 정보 수정 요청 (null 인 필드는 변경하지 않음)")
public record WearableDeviceUpdateRequest(

        @Schema(description = "새로 착용시킬 작업자 식별자", example = "2")
        Long workerId,

        @Schema(description = "true 를 보내면 작업자 배정을 해제한다. workerId 보다 우선한다", example = "false")
        Boolean unassign,

        @Schema(description = "디바이스 종류", example = "HELMET")
        DeviceType deviceType
) {
}
