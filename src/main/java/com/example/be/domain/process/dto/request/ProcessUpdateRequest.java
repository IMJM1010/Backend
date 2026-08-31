package com.example.be.domain.process.dto.request;

import com.example.be.domain.process.entity.ProcessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "공정 수정 요청 (null 인 필드는 변경하지 않음)")
public record ProcessUpdateRequest(

        @Schema(description = "공정명", example = "허브공정")
        @Size(max = 50, message = "공정명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "공정 상태", example = "STOPPED")
        ProcessStatus status
) {
}
