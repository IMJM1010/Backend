package com.example.be.domain.worker.service;

import com.example.be.domain.worker.dto.request.WorkerCreateRequest;
import com.example.be.domain.worker.dto.request.WorkerUpdateRequest;
import com.example.be.domain.worker.entity.SafetyStatus;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.worker.repository.WorkerRepository;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 작업자 도메인 서비스.
 *
 * <p>의존 방향은 작업자 → 구역 한 방향이다. 구역 쪽에서 작업자 수가 필요한 경우(삭제 가드)는
 * {@code ZoneRepository.countActiveWorkersOf} 로 해결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final ZoneService zoneService;

    /* ---------- 조회 ---------- */

    public Worker getById(Long workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKER_NOT_FOUND));
    }

    /** 필터는 모두 선택이다. 아무것도 넘기지 않으면 전체 조회. */
    public Page<Worker> getWorkers(Long zoneId, Boolean isActive, SafetyStatus safetyStatus,
                                   Pageable pageable) {
        Specification<Worker> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zone").get("id"), zoneId));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }
            if (safetyStatus != null) {
                predicates.add(cb.equal(root.get("safetyStatus"), safetyStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return workerRepository.findAll(spec, PageableUtils.withLatestFirst(pageable));
    }

    /** 구역별 작업자 목록. 구역이 없으면 404 를 내려주기 위해 존재 확인을 먼저 한다. */
    public Page<Worker> getWorkersByZoneId(Long zoneId, Pageable pageable) {
        zoneService.getById(zoneId);
        return workerRepository.findAllByZoneId(zoneId, PageableUtils.withLatestFirst(pageable));
    }

    /* ---------- 생성 / 수정 ---------- */

    @Transactional
    public Worker create(WorkerCreateRequest request) {
        if (workerRepository.existsByEmployeeNo(request.employeeNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_NO);
        }

        Zone zone = request.zoneId() != null ? zoneService.getById(request.zoneId()) : null;

        Worker worker = workerRepository.save(Worker.builder()
                .zone(zone)
                .employeeNo(request.employeeNo())
                .name(request.name())
                .build());

        log.info("작업자 등록: workerId={}, employeeNo={}", worker.getId(), worker.getEmployeeNo());
        return worker;
    }

    @Transactional
    public Worker update(Long workerId, WorkerUpdateRequest request) {
        Worker worker = getActiveWorker(workerId);
        worker.update(request.name());
        return worker;
    }

    /**
     * 안전상태 변경.
     *
     * <p>지금은 관리자 토큰으로만 호출할 수 있다. 실제로는 웨어러블 디바이스나 판정 배치가
     * 호출하는 엔드포인트이므로, 디바이스용 API Key 인증 트랙이 생기면 그쪽으로 옮긴다.
     */
    @Transactional
    public Worker changeSafetyStatus(Long workerId, SafetyStatus safetyStatus) {
        Worker worker = getActiveWorker(workerId);
        SafetyStatus before = worker.getSafetyStatus();
        worker.changeSafetyStatus(safetyStatus);

        if (before != safetyStatus) {
            log.info("안전상태 변경: workerId={}, {} -> {}", workerId, before, safetyStatus);
        }
        return worker;
    }

    /** 구역 이동. zoneId 가 null 이면 구역 미배정 처리. */
    @Transactional
    public Worker changeZone(Long workerId, Long zoneId) {
        Worker worker = getActiveWorker(workerId);
        worker.moveTo(zoneId != null ? zoneService.getById(zoneId) : null);
        return worker;
    }

    /**
     * 퇴사 처리. 행을 지우지 않고 {@code is_active = false} 로 바꾼다.
     *
     * <p>근태·생체 기록·알림 대상 매핑이 이 작업자를 참조하고 있어 실제로 지우면 이력이 깨진다.
     */
    @Transactional
    public void resign(Long workerId) {
        Worker worker = getById(workerId);
        if (!worker.isActive()) {
            throw new BusinessException(ErrorCode.WORKER_NOT_ACTIVE);
        }

        worker.resign();
        log.info("작업자 퇴사 처리: workerId={}", workerId);
    }

    /* ---------- 내부 ---------- */

    /**
     * 퇴사자에 대한 변경 요청은 막는다.
     * 퇴사한 사람의 위치나 안전상태가 갱신되면 현장 현황이 잘못 집계된다.
     */
    private Worker getActiveWorker(Long workerId) {
        Worker worker = getById(workerId);
        if (!worker.isActive()) {
            throw new BusinessException(ErrorCode.WORKER_NOT_ACTIVE);
        }
        return worker;
    }
}
