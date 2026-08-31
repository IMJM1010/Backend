package com.example.be.domain.zone.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "구역 등록 요청")
public record ZoneCreateRequest(

        @Schema(description = "소속 공정 식별자", example = "1")
        @NotNull(message = "공정 식별자는 필수입니다.")
        Long processId,

        @Schema(description = "구역 코드. 같은 공정 안에서 중복될 수 없다", example = "A-1")
        @NotBlank(message = "구역 코드는 필수입니다.")
        @Size(max = 10, message = "구역 코드는 10자 이하여야 합니다.")
        String zoneCode,

        @Schema(description = "구역명", example = "1층 자재 적치장")
        @Size(max = 50, message = "구역명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "맵 표시용 X 좌표", example = "120.50")
        BigDecimal mapX,

        @Schema(description = "맵 표시용 Y 좌표", example = "84.25")
        BigDecimal mapY
) {
}
