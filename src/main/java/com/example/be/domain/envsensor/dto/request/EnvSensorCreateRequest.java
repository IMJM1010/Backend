package com.example.be.domain.envsensor.dto.request;

import com.example.be.domain.envsensor.entity.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "환경 센서 등록 요청")
public record EnvSensorCreateRequest(

        @Schema(description = "설치 구역 식별자", example = "1")
        @NotNull(message = "구역 식별자는 필수입니다.")
        Long zoneId,

        @Schema(description = "센서 종류", example = "TEMPERATURE")
        @NotNull(message = "센서 종류는 필수입니다.")
        SensorType sensorType,

        @Schema(description = "측정 단위. 생략하면 센서 종류의 기본 단위를 쓴다", example = "℃")
        @Size(max = 10, message = "단위는 10자 이하여야 합니다.")
        String unit,

        @Schema(description = "정상 범위 하한", example = "5.00")
        @Digits(integer = 8, fraction = 2, message = "임계값은 정수 8자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal thresholdMin,

        @Schema(description = "정상 범위 상한", example = "35.00")
        @Digits(integer = 8, fraction = 2, message = "임계값은 정수 8자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal thresholdMax
) {
}
