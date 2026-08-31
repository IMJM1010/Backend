package com.example.be.domain.worker.controller;

import com.example.be.domain.worker.dto.request.SafetyStatusUpdateRequest;
import com.example.be.domain.worker.dto.request.WorkerCreateRequest;
import com.example.be.domain.worker.dto.request.WorkerUpdateRequest;
import com.example.be.domain.worker.dto.request.WorkerZoneUpdateRequest;
import com.example.be.domain.worker.dto.response.WorkerResponse;
import com.example.be.domain.worker.entity.SafetyStatus;
import com.example.be.domain.worker.service.WorkerService;
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
 * 작업자 API.
 *
 * <p>공정·구역과 달리 등록·수정은 MANAGER 도 할 수 있다. 현장 인력 배치는 일상 업무이기 때문이다.
 * 퇴사 처리(DELETE)만 ADMIN 전용이며 이 규칙은 {@code SecurityConfig} 에 있다.
 */
@Tag(name = "Workers", description = "작업자 API")
@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WorkerController {

    private final WorkerService workerService;

    @Operation(summary = "작업자 목록 조회",
            description = "zoneId, isActive, safetyStatus 로 필터링할 수 있다. 기본 정렬은 등록 최신순.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkerResponse>>> getWorkers(
            @Parameter(description = "구역 식별자 필터") @RequestParam(required = false) Long zoneId,
            @Parameter(description = "재직 여부 필터") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "안전 상태 필터") @RequestParam(required = false) SafetyStatus safetyStatus,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                workerService.getWorkers(zoneId, isActive, safetyStatus, pageable),
                WorkerResponse::from)));
    }

    @Operation(summary = "작업자 상세 조회")
    @GetMapping("/{workerId}")
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(ApiResponse.success(
                WorkerResponse.from(workerService.getById(workerId))));
    }

    @Operation(summary = "작업자 등록", description = "사번은 중복될 수 없다. zoneId 를 생략하면 미배정 상태로 등록된다.")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkerResponse>> createWorker(
            @Valid @RequestBody WorkerCreateRequest request) {

        WorkerResponse created = WorkerResponse.from(workerService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "작업자 정보 수정", description = "구역 이동과 안전상태 변경은 전용 엔드포인트를 사용한다.")
    @PatchMapping("/{workerId}")
    public ResponseEntity<ApiResponse<WorkerResponse>> updateWorker(
            @PathVariable Long workerId,
            @Valid @RequestBody WorkerUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                WorkerResponse.from(workerService.update(workerId, request))));
    }

    @Operation(summary = "작업자 안전상태 변경",
            description = "디바이스·판정 시스템 연동용. 추후 API Key 인증 트랙으로 분리 예정.")
    @PatchMapping("/{workerId}/safety-status")
    public ResponseEntity<ApiResponse<WorkerResponse>> changeSafetyStatus(
            @PathVariable Long workerId,
            @Valid @RequestBody SafetyStatusUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(WorkerResponse.from(
                workerService.changeSafetyStatus(workerId, request.safetyStatus()))));
    }

    @Operation(summary = "작업자 구역 이동", description = "zoneId 에 null 을 보내면 구역 미배정 상태가 된다.")
    @PatchMapping("/{workerId}/zone")
    public ResponseEntity<ApiResponse<WorkerResponse>> changeZone(
            @PathVariable Long workerId,
            @RequestBody WorkerZoneUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                WorkerResponse.from(workerService.changeZone(workerId, request.zoneId()))));
    }

    @Operation(summary = "작업자 퇴사 처리",
            description = "실제로 삭제하지 않고 is_active 를 false 로 바꾸며 구역에서도 제외한다. ADMIN 전용.")
    @DeleteMapping("/{workerId}")
    public ResponseEntity<Void> resignWorker(@PathVariable Long workerId) {
        workerService.resign(workerId);
        return ResponseEntity.noContent().build();
    }
}
