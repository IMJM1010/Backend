package com.example.be.domain.worker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 작업자 정보 수정.
 *
 * <p>구역 이동과 안전상태 변경은 전용 엔드포인트가 따로 있다.
 * 위치와 안전상태는 디바이스 연동으로도 바뀌는 값이라 일반 정보 수정과 성격이 다르다.
 */
@Schema(description = "작업자 정보 수정 요청 (null 인 필드는 변경하지 않음)")
public record WorkerUpdateRequest(

        @Schema(description = "작업자명", example = "김철수")
        @Size(max = 30, message = "작업자명은 30자 이하여야 합니다.")
        String name
) {
}
