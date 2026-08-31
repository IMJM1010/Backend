package com.example.be.domain.vitalrecord.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 생체 기록 수집 요청. 디바이스·게이트웨이가 호출한다.
 *
 * <p>측정값의 상·하한을 검증하는 이유는 센서 오류로 들어온 값이 그대로 저장되면
 * 알림 판정이 오작동하기 때문이다. (심박수 0, 체온 300℃ 같은 값)
 */
@Schema(description = "생체 기록 수집 요청")
public record VitalRecordCreateRequest(

        @Schema(description = "측정한 디바이스 식별자", example = "1")
        @NotNull(message = "디바이스 식별자는 필수입니다.")
        Long deviceId,

        @Schema(description = "심박수 (bpm)", example = "78")
        @Min(value = 20, message = "심박수가 측정 가능 범위를 벗어났습니다.")
        @Max(value = 250, message = "심박수가 측정 가능 범위를 벗어났습니다.")
        Integer heartRate,

        @Schema(description = "체온 (℃)", example = "36.5")
        @DecimalMin(value = "30.0", message = "체온이 측정 가능 범위를 벗어났습니다.")
        @DecimalMax(value = "45.0", message = "체온이 측정 가능 범위를 벗어났습니다.")
        BigDecimal bodyTemp,

        @Schema(description = "걸음 수", example = "4210")
        @PositiveOrZero(message = "걸음 수는 0 이상이어야 합니다.")
        Integer stepCount,

        @Schema(description = "측정 일시", example = "2026-08-31T14:30:00")
        @NotNull(message = "측정 일시는 필수입니다.")
        LocalDateTime measuredAt
) {
}
