package com.example.be.domain.worker.dto.request;

import com.example.be.domain.worker.entity.SafetyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "작업자 안전상태 변경 요청")
public record SafetyStatusUpdateRequest(

        @Schema(description = "안전 상태", example = "DANGER")
        @NotNull(message = "안전 상태는 필수입니다.")
        SafetyStatus safetyStatus
) {
}
