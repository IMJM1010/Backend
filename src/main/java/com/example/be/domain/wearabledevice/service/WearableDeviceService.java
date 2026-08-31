package com.example.be.domain.wearabledevice.service;

import com.example.be.domain.wearabledevice.dto.request.DeviceStatusUpdateRequest;
import com.example.be.domain.wearabledevice.dto.request.WearableDeviceCreateRequest;
import com.example.be.domain.wearabledevice.dto.request.WearableDeviceUpdateRequest;
import com.example.be.domain.wearabledevice.entity.ConnectionStatus;
import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.wearabledevice.repository.WearableDeviceRepository;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.worker.service.WorkerService;
import com.example.be.global.common.PageableUtils;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 웨어러블 디바이스 도메인 서비스.
 *
 * <p>의존 방향은 디바이스 → 작업자 한 방향이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WearableDeviceService {

    private final WearableDeviceRepository deviceRepository;
    private final WorkerService workerService;

    /* ---------- 조회 ---------- */

    public WearableDevice getById(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
    }

    public Page<WearableDevice> getDevices(Long workerId, ConnectionStatus connectionStatus,
                                           Pageable pageable) {
        Specification<WearableDevice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (workerId != null) {
                predicates.add(cb.equal(root.get("worker").get("id"), workerId));
            }
            if (connectionStatus != null) {
                predicates.add(cb.equal(root.get("connectionStatus"), connectionStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return deviceRepository.findAll(spec, PageableUtils.withLatestFirst(pageable));
    }

    /** 작업자가 착용 중인 디바이스 목록. 작업자가 없으면 404. */
    public List<WearableDevice> getDevicesByWorkerId(Long workerId) {
        workerService.getById(workerId);
        return deviceRepository.findAllByWorkerId(workerId);
    }

    /* ---------- 생성 / 수정 ---------- */

    @Transactional
    public WearableDevice create(WearableDeviceCreateRequest request) {
        if (deviceRepository.existsBySerialNo(request.serialNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NO);
        }

        Worker worker = null;
        if (request.workerId() != null) {
            worker = getAssignableWorker(request.workerId());
            if (deviceRepository.existsByWorkerIdAndDeviceType(worker.getId(), request.deviceType())) {
                throw new BusinessException(ErrorCode.DUPLICATE_DEVICE_TYPE_FOR_WORKER);
            }
        }

        WearableDevice device = deviceRepository.save(WearableDevice.builder()
                .worker(worker)
                .serialNo(request.serialNo())
                .deviceType(request.deviceType())
                .build());

        log.info("디바이스 등록: deviceId={}, serialNo={}", device.getId(), device.getSerialNo());
        return device;
    }

    @Transactional
    public WearableDevice update(Long deviceId, WearableDeviceUpdateRequest request) {
        WearableDevice device = getActiveDevice(deviceId);

        device.changeType(request.deviceType());

        if (Boolean.TRUE.equals(request.unassign())) {
            device.assignTo(null);
        } else if (request.workerId() != null) {
            Worker worker = getAssignableWorker(request.workerId());
            DeviceType targetType = device.getDeviceType();
            if (deviceRepository.existsByWorkerIdAndDeviceTypeAndIdNot(
                    worker.getId(), targetType, device.getId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_DEVICE_TYPE_FOR_WORKER);
            }
            device.assignTo(worker);
        }

        return device;
    }

    /**
     * 상태 갱신. 게이트웨이가 주기적으로 호출한다.
     *
     * <p>지금은 관리자 JWT 로만 열려 있다. 디바이스용 API Key 인증 트랙이 생기면 그쪽으로 옮긴다.
     */
    @Transactional
    public WearableDevice updateStatus(Long deviceId, DeviceStatusUpdateRequest request) {
        WearableDevice device = getActiveDevice(deviceId);

        // RETIRED 는 폐기 API 로만 진입할 수 있다. 게이트웨이가 실수로 보내도 무시한다.
        ConnectionStatus status = request.connectionStatus() == ConnectionStatus.RETIRED
                ? null
                : request.connectionStatus();

        device.updateStatus(request.batteryLevel(), status, request.lastSyncedAt());
        return device;
    }

    /**
     * 폐기·분실 처리. 행을 지우지 않고 상태만 바꾼다.
     *
     * <p>이 디바이스가 남긴 생체 기록이 device_id 로 참조하고 있어 실제로 지우면
     * 과거 측정 이력을 통째로 잃는다.
     */
    @Transactional
    public void retire(Long deviceId) {
        WearableDevice device = getById(deviceId);
        if (device.isRetired()) {
            throw new BusinessException(ErrorCode.DEVICE_ALREADY_RETIRED);
        }

        device.retire();
        log.info("디바이스 폐기 처리: deviceId={}", deviceId);
    }

    /* ---------- 내부 ---------- */

    private WearableDevice getActiveDevice(Long deviceId) {
        WearableDevice device = getById(deviceId);
        if (device.isRetired()) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        return device;
    }

    /** 퇴사자에게는 디바이스를 배정하지 않는다. */
    private Worker getAssignableWorker(Long workerId) {
        Worker worker = workerService.getById(workerId);
        if (!worker.isActive()) {
            throw new BusinessException(ErrorCode.WORKER_NOT_ACTIVE);
        }
        return worker;
    }
}
