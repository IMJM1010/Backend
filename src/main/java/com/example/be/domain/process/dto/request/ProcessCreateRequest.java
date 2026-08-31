package com.example.be.domain.process.dto.request;

import com.example.be.domain.process.entity.ProcessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "공정 등록 요청")
public record ProcessCreateRequest(

        @Schema(description = "공정명", example = "허브공정")
        @NotBlank(message = "공정명은 필수입니다.")
        @Size(max = 50, message = "공정명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "공정 상태. 생략하면 RUNNING", example = "RUNNING")
        ProcessStatus status
) {
}
