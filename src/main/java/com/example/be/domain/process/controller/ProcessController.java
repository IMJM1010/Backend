package com.example.be.domain.process.controller;

import com.example.be.domain.process.dto.request.ProcessCreateRequest;
import com.example.be.domain.process.dto.request.ProcessUpdateRequest;
import com.example.be.domain.process.dto.response.ProcessResponse;
import com.example.be.domain.process.entity.ProcessStatus;
import com.example.be.domain.process.service.ProcessService;
import com.example.be.domain.zone.dto.response.ZoneResponse;
import com.example.be.domain.zone.service.ZoneService;
import com.example.be.global.common.ApiResponse;
import com.example.be.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공정 API.
 *
 * <p>{@code /processes/{id}/zones} 는 구역 도메인의 데이터를 내려주므로 {@link ZoneService} 를 쓴다.
 * 판단 로직은 서비스에 있고 여기서는 어느 서비스로 보낼지만 정한다.
 *
 * <p>등록·수정·삭제의 ADMIN 검사는 {@code SecurityConfig} 에서 처리한다.
 */
@Tag(name = "Processes", description = "공정 API")
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProcessController {

    private final ProcessService processService;
    private final ZoneService zoneService;

    @Operation(summary = "공정 목록 조회", description = "status 로 필터링할 수 있다. 기본 정렬은 등록 최신순.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProcessResponse>>> getProcesses(
            @Parameter(description = "공정 상태 필터") @RequestParam(required = false) ProcessStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(processService.getProcesses(status, pageable), ProcessResponse::from)));
    }

    @Operation(summary = "공정 상세 조회")
    @GetMapping("/{processId}")
    public ResponseEntity<ApiResponse<ProcessResponse>> getProcess(@PathVariable Long processId) {
        return ResponseEntity.ok(ApiResponse.success(
                ProcessResponse.from(processService.getById(processId))));
    }

    @Operation(summary = "공정 등록", description = "ADMIN 만 호출할 수 있다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProcessResponse>> createProcess(
            @Valid @RequestBody ProcessCreateRequest request) {

        ProcessResponse created = ProcessResponse.from(processService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "공정 정보 수정", description = "ADMIN 만 호출할 수 있다.")
    @PatchMapping("/{processId}")
    public ResponseEntity<ApiResponse<ProcessResponse>> updateProcess(
            @PathVariable Long processId,
            @Valid @RequestBody ProcessUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                ProcessResponse.from(processService.update(processId, request))));
    }

    @Operation(summary = "공정 삭제",
            description = "하위 구역이 남아 있으면 409 를 반환한다. 구역을 먼저 정리해야 한다. ADMIN 전용.")
    @DeleteMapping("/{processId}")
    public ResponseEntity<Void> deleteProcess(@PathVariable Long processId) {
        processService.delete(processId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공정별 구역 목록 조회")
    @GetMapping("/{processId}/zones")
    public ResponseEntity<ApiResponse<PageResponse<ZoneResponse>>> getZonesByProcess(
            @PathVariable Long processId, Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(zoneService.getZonesByProcessId(processId, pageable),
                        ZoneResponse::from)));
    }
}
