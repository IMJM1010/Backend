package com.example.be.domain.vitalrecord.service;

import com.example.be.domain.vitalrecord.dto.request.VitalRecordCreateRequest;
import com.example.be.domain.vitalrecord.entity.VitalRecord;
import com.example.be.domain.vitalrecord.repository.VitalRecordRepository;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.wearabledevice.service.WearableDeviceService;
import com.example.be.domain.worker.service.WorkerService;
import com.example.be.global.common.PageableUtils;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 생체 기록 도메인 서비스.
 *
 * <p>이 도메인은 다른 도메인과 다르게 <b>조회 자체를 제약</b>한다.
 * 데이터가 수천만 건 단위로 쌓이기 때문에, 편의를 위해 조회를 열어두면
 * 프론트의 실수 한 번으로 서버가 멈춘다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VitalRecordService {

    /** 한 번에 조회할 수 있는 최대 기간. 넘으면 400 으로 거절한다. */
    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(7);

    private final VitalRecordRepository vitalRecordRepository;
    private final WearableDeviceService deviceService;
    private final WorkerService workerService;

    /* ---------- 조회 ---------- */

    public VitalRecord getById(Long vitalRecordId) {
        return vitalRecordRepository.findById(vitalRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VITAL_RECORD_NOT_FOUND));
    }

    /**
     * 기간 조회. {@code from}, {@code to} 는 컨트롤러에서 필수로 강제한다.
     *
     * <p>deviceId 가 있으면 인덱스를 그대로 타고, 없으면 기간만으로 훑는다.
     * 후자는 대시보드 집계용이므로 기간 상한이 특히 중요하다.
     */
    public Page<VitalRecord> getVitalRecords(Long deviceId, LocalDateTime from, LocalDateTime to,
                                             Pageable pageable) {
        validatePeriod(from, to);

        Pageable sorted = PageableUtils.withDefaultSort(pageable, "measuredAt", Sort.Direction.DESC);
        return deviceId == null
                ? vitalRecordRepository.findAllByMeasuredAtBetween(from, to, sorted)
                : vitalRecordRepository.findAllByDeviceIdAndMeasuredAtBetween(deviceId, from, to, sorted);
    }

    /** 디바이스별 기록. 디바이스가 없으면 404. */
    public Page<VitalRecord> getVitalRecordsByDevice(Long deviceId, LocalDateTime from,
                                                     LocalDateTime to, Pageable pageable) {
        deviceService.getById(deviceId);
        return getVitalRecords(deviceId, from, to, pageable);
    }

    /**
     * 특정 작업자의 최신 생체 데이터 1건.
     *
     * <p>대시보드가 주기적으로 폴링하는 엔드포인트라 가장 자주 호출된다.
     * 반드시 1건만 가져오도록 {@code PageRequest.of(0, 1)} 을 넘긴다.
     */
    public VitalRecord getLatestByWorker(Long workerId) {
        workerService.getById(workerId);

        List<VitalRecord> latest =
                vitalRecordRepository.findLatestByWorkerId(workerId, PageRequest.of(0, 1));

        if (latest.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_VITAL_RECORD);
        }
        return latest.getFirst();
    }

    /* ---------- 수집 / 삭제 ---------- */

    /**
     * 생체 기록 수집.
     *
     * <p>지금은 관리자 JWT 로 열려 있다. 실제로는 디바이스·게이트웨이가 호출하므로
     * API Key 인증 트랙이 생기면 그쪽으로 옮긴다.
     */
    @Transactional
    public VitalRecord create(VitalRecordCreateRequest request) {
        WearableDevice device = deviceService.getById(request.deviceId());
        if (device.isRetired()) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }

        // 시계가 어긋난 게이트웨이가 미래 시각을 보내면 기간 조회에서 영영 잡히지 않는다.
        if (request.measuredAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException(ErrorCode.FUTURE_MEASURED_AT);
        }

        return vitalRecordRepository.save(VitalRecord.builder()
                .device(device)
                .heartRate(request.heartRate())
                .bodyTemp(request.bodyTemp())
                .stepCount(request.stepCount())
                .measuredAt(request.measuredAt())
                .build());
    }

    /** 오기록 삭제. 이력 성격이라 ADMIN 만 호출할 수 있게 SecurityConfig 에서 제한한다. */
    @Transactional
    public void delete(Long vitalRecordId) {
        VitalRecord record = getById(vitalRecordId);
        vitalRecordRepository.delete(record);
        log.info("생체 기록 삭제: vitalRecordId={}", vitalRecordId);
    }

    /* ---------- 내부 ---------- */

    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_PERIOD);
        }
        if (Duration.between(from, to).compareTo(MAX_QUERY_RANGE) > 0) {
            throw new BusinessException(ErrorCode.PERIOD_TOO_LONG,
                    "조회 기간은 최대 %d일까지 가능합니다.".formatted(MAX_QUERY_RANGE.toDays()));
        }
    }
}
