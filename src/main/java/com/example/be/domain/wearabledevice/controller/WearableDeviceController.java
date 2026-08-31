package com.example.be.domain.wearabledevice.controller;

import com.example.be.domain.vitalrecord.dto.response.VitalRecordResponse;
import com.example.be.domain.vitalrecord.service.VitalRecordService;
import com.example.be.domain.wearabledevice.dto.request.DeviceStatusUpdateRequest;
import com.example.be.domain.wearabledevice.dto.request.WearableDeviceCreateRequest;
import com.example.be.domain.wearabledevice.dto.request.WearableDeviceUpdateRequest;
import com.example.be.domain.wearabledevice.dto.response.WearableDeviceResponse;
import com.example.be.domain.wearabledevice.entity.ConnectionStatus;
import com.example.be.domain.wearabledevice.service.WearableDeviceService;
import com.example.be.global.common.ApiResponse;
import com.example.be.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;

/**
 * 웨어러블 디바이스 API.
 *
 * <p>등록·수정·폐기는 ADMIN 전용이고, 상태 갱신({@code /status})만 모든 관리자가 호출할 수 있다.
 * 상태 갱신은 사람이 아니라 게이트웨이가 부르는 엔드포인트이기 때문이며,
 * API Key 인증 트랙이 생기면 그쪽으로 옮긴다.
 */
@Tag(name = "Wearable Devices", description = "웨어러블 디바이스 API")
@RestController
@RequestMapping("/api/wearable-devices")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WearableDeviceController {

    private final WearableDeviceService deviceService;
    private final VitalRecordService vitalRecordService;

    @Operation(summary = "디바이스 목록 조회", description = "workerId, connectionStatus 로 필터링할 수 있다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WearableDeviceResponse>>> getDevices(
            @Parameter(description = "착용 작업자 필터") @RequestParam(required = false) Long workerId,
            @Parameter(description = "연결 상태 필터") @RequestParam(required = false) ConnectionStatus connectionStatus,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                deviceService.getDevices(workerId, connectionStatus, pageable),
                WearableDeviceResponse::from)));
    }

    @Operation(summary = "디바이스 상세 조회")
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<WearableDeviceResponse>> getDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.success(
                WearableDeviceResponse.from(deviceService.getById(deviceId))));
    }

    @Operation(summary = "디바이스 등록",
            description = "시리얼 번호는 중복될 수 없다. 한 작업자가 같은 종류를 둘 이상 착용할 수 없다. ADMIN 전용.")
    @PostMapping
    public ResponseEntity<ApiResponse<WearableDeviceResponse>> createDevice(
            @Valid @RequestBody WearableDeviceCreateRequest request) {

        WearableDeviceResponse created = WearableDeviceResponse.from(deviceService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "디바이스 정보 수정",
            description = "착용 작업자와 종류를 변경한다. unassign=true 를 보내면 배정을 해제한다. ADMIN 전용.")
    @PatchMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<WearableDeviceResponse>> updateDevice(
            @PathVariable Long deviceId,
            @Valid @RequestBody WearableDeviceUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                WearableDeviceResponse.from(deviceService.update(deviceId, request))));
    }

    @Operation(summary = "디바이스 상태 갱신",
            description = "배터리 잔량·연결 상태·동기화 시각을 갱신한다. 게이트웨이 연동용. "
                    + "connectionStatus 에 RETIRED 를 보내도 무시된다. (폐기는 DELETE 로만)")
    @PatchMapping("/{deviceId}/status")
    public ResponseEntity<ApiResponse<WearableDeviceResponse>> updateStatus(
            @PathVariable Long deviceId,
            @Valid @RequestBody DeviceStatusUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                WearableDeviceResponse.from(deviceService.updateStatus(deviceId, request))));
    }

    @Operation(summary = "디바이스 폐기·분실 처리",
            description = "실제로 삭제하지 않고 connectionStatus 를 RETIRED 로 바꾸며 작업자 배정을 해제한다. ADMIN 전용.")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> retireDevice(@PathVariable Long deviceId) {
        deviceService.retire(deviceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "디바이스별 생체 기록 조회",
            description = "from, to 는 필수다. 조회 기간은 최대 7일까지 허용한다.")
    @GetMapping("/{deviceId}/vital-records")
    public ResponseEntity<ApiResponse<PageResponse<VitalRecordResponse>>> getVitalRecords(
            @PathVariable Long deviceId,
            @Parameter(description = "조회 시작 일시", required = true, example = "2026-08-24T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "조회 종료 일시", required = true, example = "2026-08-31T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                vitalRecordService.getVitalRecordsByDevice(deviceId, from, to, pageable),
                VitalRecordResponse::from)));
    }
}
