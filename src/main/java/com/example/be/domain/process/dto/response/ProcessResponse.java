package com.example.be.domain.process.dto.response;

import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.entity.ProcessStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공정 응답")
public record ProcessResponse(
        Long processId,
        String name,
        ProcessStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProcessResponse from(Process process) {
        return new ProcessResponse(
                process.getId(),
                process.getName(),
                process.getStatus(),
                process.getCreatedAt(),
                process.getUpdatedAt()
        );
    }
}
