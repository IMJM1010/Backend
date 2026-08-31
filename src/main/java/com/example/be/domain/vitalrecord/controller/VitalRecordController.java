package com.example.be.domain.vitalrecord.controller;

import com.example.be.domain.vitalrecord.dto.request.VitalRecordCreateRequest;
import com.example.be.domain.vitalrecord.dto.response.VitalRecordResponse;
import com.example.be.domain.vitalrecord.service.VitalRecordService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 생체 기록 API.
 *
 * <p>목록 조회에 {@code from}, {@code to} 를 <b>필수</b>로 둔 것은 의도적이다.
 * 이 테이블은 작업자 100명 기준 하루 수십만 건이 쌓이므로, 기간 없이 조회를 허용하면
 * 프론트의 실수 한 번으로 서버가 멈춘다. 기간 상한은 7일이다.
 */
@Tag(name = "Vital Records", description = "생체 기록 API")
@RestController
@RequestMapping("/api/vital-records")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VitalRecordController {

    private final VitalRecordService vitalRecordService;

    @Operation(summary = "생체 기록 목록 조회",
            description = "from, to 는 필수이며 최대 7일까지 조회할 수 있다. deviceId 로 좁히면 인덱스를 그대로 탄다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VitalRecordResponse>>> getVitalRecords(
            @Parameter(description = "디바이스 식별자 필터") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "조회 시작 일시", required = true, example = "2026-08-31T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "조회 종료 일시", required = true, example = "2026-08-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                vitalRecordService.getVitalRecords(deviceId, from, to, pageable),
                VitalRecordResponse::from)));
    }

    @Operation(summary = "실시간 생체 데이터 조회",
            description = "특정 작업자의 가장 최근 측정값 1건을 반환한다. 대시보드 폴링용.")
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<VitalRecordResponse>> getRealtime(
            @Parameter(description = "작업자 식별자", required = true) @RequestParam Long workerId) {

        return ResponseEntity.ok(ApiResponse.success(
                VitalRecordResponse.from(vitalRecordService.getLatestByWorker(workerId))));
    }

    @Operation(summary = "생체 기록 상세 조회")
    @GetMapping("/{vitalRecordId}")
    public ResponseEntity<ApiResponse<VitalRecordResponse>> getVitalRecord(
            @PathVariable Long vitalRecordId) {

        return ResponseEntity.ok(ApiResponse.success(
                VitalRecordResponse.from(vitalRecordService.getById(vitalRecordId))));
    }

    @Operation(summary = "생체 기록 수집",
            description = "디바이스·게이트웨이가 호출한다. 추후 API Key 인증 트랙으로 분리 예정.")
    @PostMapping
    public ResponseEntity<ApiResponse<VitalRecordResponse>> createVitalRecord(
            @Valid @RequestBody VitalRecordCreateRequest request) {

        VitalRecordResponse created = VitalRecordResponse.from(vitalRecordService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "생체 기록 삭제", description = "센서 오류로 들어온 오기록을 지운다. ADMIN 전용.")
    @DeleteMapping("/{vitalRecordId}")
    public ResponseEntity<Void> deleteVitalRecord(@PathVariable Long vitalRecordId) {
        vitalRecordService.delete(vitalRecordId);
        return ResponseEntity.noContent().build();
    }
}
