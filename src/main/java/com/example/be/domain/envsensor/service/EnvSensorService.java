package com.example.be.domain.envsensor.service;

import com.example.be.domain.envsensor.dto.request.EnvSensorCreateRequest;
import com.example.be.domain.envsensor.dto.request.EnvSensorUpdateRequest;
import com.example.be.domain.envsensor.entity.EnvSensor;
import com.example.be.domain.envsensor.entity.SensorType;
import com.example.be.domain.envsensor.repository.EnvSensorRepository;
import com.example.be.domain.zone.entity.Zone;
import com.example.be.domain.zone.service.ZoneService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 환경 센서 도메인 서비스.
 *
 * <p>의존 방향은 센서 → 구역 한 방향이다. 구역 쪽에서 센서가 필요한 경우(삭제 가드)는
 * {@code ZoneRepository.countSensorsOf} 로 해결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvSensorService {

    private final EnvSensorRepository sensorRepository;
    private final ZoneService zoneService;

    /* ---------- 조회 ---------- */

    public EnvSensor getById(Long sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SENSOR_NOT_FOUND));
    }

    /** 필터는 모두 선택이다. 아무것도 넘기지 않으면 전체 조회. */
    public Page<EnvSensor> getSensors(Long zoneId, SensorType sensorType, Pageable pageable) {
        Specification<EnvSensor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zone").get("id"), zoneId));
            }
            if (sensorType != null) {
                predicates.add(cb.equal(root.get("sensorType"), sensorType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return sensorRepository.findAll(spec, PageableUtils.withLatestFirst(pageable));
    }

    /** 구역에 설치된 센서 목록. 구역이 없으면 404 를 내려주기 위해 존재 확인을 먼저 한다. */
    public Page<EnvSensor> getSensorsByZoneId(Long zoneId, Pageable pageable) {
        zoneService.getById(zoneId);
        return getSensors(zoneId, null, pageable);
    }

    /* ---------- 생성 / 수정 / 삭제 ---------- */

    @Transactional
    public EnvSensor create(EnvSensorCreateRequest request) {
        Zone zone = zoneService.getById(request.zoneId());
        validateThreshold(request.thresholdMin(), request.thresholdMax());

        EnvSensor sensor = sensorRepository.save(EnvSensor.builder()
                .zone(zone)
                .sensorType(request.sensorType())
                .unit(request.unit())
                .thresholdMin(request.thresholdMin())
                .thresholdMax(request.thresholdMax())
                .build());

        log.info("환경 센서 등록: sensorId={}, zoneId={}, type={}",
                sensor.getId(), zone.getId(), sensor.getSensorType());
        return sensor;
    }

    @Transactional
    public EnvSensor update(Long sensorId, EnvSensorUpdateRequest request) {
        EnvSensor sensor = getById(sensorId);

        // 하한만 또는 상한만 보내는 경우가 많아서, 기존 값과 합친 결과로 범위를 검사한다.
        BigDecimal min = request.thresholdMin() != null ? request.thresholdMin() : sensor.getThresholdMin();
        BigDecimal max = request.thresholdMax() != null ? request.thresholdMax() : sensor.getThresholdMax();
        validateThreshold(min, max);

        if (request.zoneId() != null && !request.zoneId().equals(sensor.getZone().getId())) {
            sensor.moveTo(zoneService.getById(request.zoneId()));
        }
        sensor.changeType(request.sensorType(), request.unit());
        sensor.changeThreshold(request.thresholdMin(), request.thresholdMax());
        return sensor;
    }

    /**
     * 철거 처리. 지금은 행을 지운다.
     *
     * <p>환경 기록(env_records)이 붙으면 센서를 지울 때 측정 이력이 고아가 된다.
     * 그 도메인을 만들 때 "기록이 있으면 409" 가드를 여기에 추가한다.
     */
    @Transactional
    public void delete(Long sensorId) {
        EnvSensor sensor = getById(sensorId);
        sensorRepository.delete(sensor);
        log.info("환경 센서 삭제: sensorId={}", sensorId);
    }

    /* ---------- 내부 ---------- */

    private void validateThreshold(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.INVALID_THRESHOLD_RANGE);
        }
    }
}
