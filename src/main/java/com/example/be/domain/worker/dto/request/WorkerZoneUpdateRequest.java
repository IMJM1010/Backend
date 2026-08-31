package com.example.be.domain.worker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작업자 구역 이동 요청")
public record WorkerZoneUpdateRequest(

        @Schema(description = "이동할 구역 식별자. null 을 보내면 구역 미배정 상태가 된다", example = "2")
        Long zoneId
) {
}
