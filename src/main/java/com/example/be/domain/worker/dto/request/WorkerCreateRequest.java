package com.example.be.domain.worker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "작업자 등록 요청")
public record WorkerCreateRequest(

        @Schema(description = "배정할 구역 식별자. 생략하면 미배정 상태로 등록된다", example = "1")
        Long zoneId,

        @Schema(description = "사번. 중복될 수 없다", example = "2026-0001")
        @NotBlank(message = "사번은 필수입니다.")
        @Size(max = 30, message = "사번은 30자 이하여야 합니다.")
        String employeeNo,

        @Schema(description = "작업자명", example = "김철수")
        @NotBlank(message = "작업자명은 필수입니다.")
        @Size(max = 30, message = "작업자명은 30자 이하여야 합니다.")
        String name
) {
}
