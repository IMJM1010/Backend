package com.example.be.domain.zone.controller;

import com.example.be.domain.zone.dto.request.ZoneCreateRequest;
import com.example.be.domain.zone.dto.request.ZoneUpdateRequest;
import com.example.be.domain.zone.dto.response.ZoneResponse;
import com.example.be.domain.worker.dto.response.WorkerResponse;
import com.example.be.domain.worker.service.WorkerService;
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
 * 구역 API. 등록·수정·삭제의 ADMIN 검사는 {@code SecurityConfig} 에서 처리한다.
 */
@Tag(name = "Zones", description = "구역 API")
@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ZoneController {

    private final ZoneService zoneService;
    private final WorkerService workerService;

    @Operation(summary = "구역 목록 조회", description = "processId 로 필터링할 수 있다. 기본 정렬은 구역 코드 오름차순.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ZoneResponse>>> getZones(
            @Parameter(description = "공정 식별자 필터") @RequestParam(required = false) Long processId,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(zoneService.getZones(processId, pageable), ZoneResponse::from)));
    }

    @Operation(summary = "구역 상세 조회", description = "map_x, map_y 좌표를 포함한다.")
    @GetMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneResponse>> getZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(ApiResponse.success(ZoneResponse.from(zoneService.getById(zoneId))));
    }

    @Operation(summary = "구역 등록",
            description = "구역 코드는 같은 공정 안에서 중복될 수 없다. ADMIN 전용.")
    @PostMapping
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
            @Valid @RequestBody ZoneCreateRequest request) {

        ZoneResponse created = ZoneResponse.from(zoneService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "구역 정보 수정",
            description = "processId 를 넘기면 구역을 다른 공정으로 옮긴다. ADMIN 전용.")
    @PatchMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneResponse>> updateZone(
            @PathVariable Long zoneId,
            @Valid @RequestBody ZoneUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                ZoneResponse.from(zoneService.update(zoneId, request))));
    }

    @Operation(summary = "구역 내 작업자 목록 조회", description = "현재 해당 구역에 배정된 작업자를 조회한다.")
    @GetMapping("/{zoneId}/workers")
    public ResponseEntity<ApiResponse<PageResponse<WorkerResponse>>> getWorkersByZone(
            @PathVariable Long zoneId, Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                workerService.getWorkersByZoneId(zoneId, pageable), WorkerResponse::from)));
    }

    @Operation(summary = "구역 삭제",
            description = "재직 중인 작업자가 남아 있으면 409 를 반환한다. ADMIN 전용.")
    @DeleteMapping("/{zoneId}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long zoneId) {
        zoneService.delete(zoneId);
        return ResponseEntity.noContent().build();
    }
}
