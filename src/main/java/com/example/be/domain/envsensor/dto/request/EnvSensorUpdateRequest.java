package com.example.be.domain.envsensor.dto.request;

import com.example.be.domain.envsensor.entity.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "환경 센서 수정 요청 (null 인 필드는 변경하지 않음)")
public record EnvSensorUpdateRequest(

        @Schema(description = "옮겨 설치할 구역 식별자", example = "2")
        Long zoneId,

        @Schema(description = "센서 종류. 단위를 함께 보내지 않으면 새 종류의 기본 단위로 바뀐다", example = "NOISE")
        SensorType sensorType,

        @Schema(description = "측정 단위", example = "dB")
        @Size(max = 10, message = "단위는 10자 이하여야 합니다.")
        String unit,

        @Schema(description = "정상 범위 하한", example = "0.00")
        @Digits(integer = 8, fraction = 2, message = "임계값은 정수 8자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal thresholdMin,

        @Schema(description = "정상 범위 상한", example = "85.00")
        @Digits(integer = 8, fraction = 2, message = "임계값은 정수 8자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal thresholdMax
) {
}
