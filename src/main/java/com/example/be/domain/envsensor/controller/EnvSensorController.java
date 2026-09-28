package com.example.be.domain.envsensor.controller;

import com.example.be.domain.envsensor.dto.request.EnvSensorCreateRequest;
import com.example.be.domain.envsensor.dto.request.EnvSensorUpdateRequest;
import com.example.be.domain.envsensor.dto.response.EnvSensorResponse;
import com.example.be.domain.envsensor.entity.SensorType;
import com.example.be.domain.envsensor.service.EnvSensorService;
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
 * 환경 센서 API. 등록·수정·삭제의 ADMIN 검사는 {@code SecurityConfig} 에서 처리한다.
 *
 * <p>센서별 측정 기록 조회({@code GET /api/env-sensors/{id}/records})는 환경 기록 도메인과 함께 붙인다.
 */
@Tag(name = "Env Sensors", description = "환경 센서 API")
@RestController
@RequestMapping("/api/env-sensors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EnvSensorController {

    private final EnvSensorService sensorService;

    @Operation(summary = "환경 센서 목록 조회", description = "zoneId, sensorType 으로 필터링할 수 있다. 기본 정렬은 등록 최신순.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EnvSensorResponse>>> getSensors(
            @Parameter(description = "설치 구역 필터") @RequestParam(required = false) Long zoneId,
            @Parameter(description = "센서 종류 필터") @RequestParam(required = false) SensorType sensorType,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                sensorService.getSensors(zoneId, sensorType, pageable), EnvSensorResponse::from)));
    }

    @Operation(summary = "환경 센서 상세 조회", description = "임계값(threshold_min/max)을 포함한다.")
    @GetMapping("/{sensorId}")
    public ResponseEntity<ApiResponse<EnvSensorResponse>> getSensor(@PathVariable Long sensorId) {
        return ResponseEntity.ok(ApiResponse.success(
                EnvSensorResponse.from(sensorService.getById(sensorId))));
    }

    @Operation(summary = "환경 센서 등록",
            description = "unit 을 생략하면 센서 종류의 기본 단위를 쓴다. 임계값 하한은 상한보다 클 수 없다. ADMIN 전용.")
    @PostMapping
    public ResponseEntity<ApiResponse<EnvSensorResponse>> createSensor(
            @Valid @RequestBody EnvSensorCreateRequest request) {

        EnvSensorResponse created = EnvSensorResponse.from(sensorService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "환경 센서 정보 수정",
            description = "임계값, 설치 구역, 종류를 수정한다. 임계값은 기존 값과 합친 결과로 범위를 검사한다. ADMIN 전용.")
    @PatchMapping("/{sensorId}")
    public ResponseEntity<ApiResponse<EnvSensorResponse>> updateSensor(
            @PathVariable Long sensorId,
            @Valid @RequestBody EnvSensorUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                EnvSensorResponse.from(sensorService.update(sensorId, request))));
    }

    @Operation(summary = "환경 센서 삭제", description = "철거 처리. ADMIN 전용.")
    @DeleteMapping("/{sensorId}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long sensorId) {
        sensorService.delete(sensorId);
        return ResponseEntity.noContent().build();
    }
}
