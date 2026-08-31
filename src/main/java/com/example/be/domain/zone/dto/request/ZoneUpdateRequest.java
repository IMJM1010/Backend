package com.example.be.domain.zone.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "구역 수정 요청 (null 인 필드는 변경하지 않음)")
public record ZoneUpdateRequest(

        @Schema(description = "옮길 공정 식별자. 구역을 다른 공정으로 이동할 때만 사용", example = "2")
        Long processId,

        @Schema(description = "구역 코드", example = "A-2")
        @Size(max = 10, message = "구역 코드는 10자 이하여야 합니다.")
        String zoneCode,

        @Schema(description = "구역명", example = "1층 자재 적치장")
        @Size(max = 50, message = "구역명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "맵 표시용 X 좌표")
        BigDecimal mapX,

        @Schema(description = "맵 표시용 Y 좌표")
        BigDecimal mapY
) {
}
