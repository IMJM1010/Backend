package com.example.be.domain.zone.service;

import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.service.ProcessService;
import com.example.be.domain.zone.dto.request.ZoneCreateRequest;
import com.example.be.domain.zone.dto.request.ZoneUpdateRequest;
import com.example.be.domain.zone.entity.Zone;
import com.example.be.domain.zone.repository.ZoneRepository;
import com.example.be.global.common.PageableUtils;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구역 도메인 서비스.
 *
 * <p>공정 조회는 {@link ProcessService} 에 위임한다. 의존 방향은 구역 → 공정 한 방향만 두고,
 * 공정 쪽에서 구역이 필요한 경우(삭제 가드)는 {@code ProcessRepository.hasZones} 로 해결해
 * 두 서비스가 서로를 참조하는 순환을 만들지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ProcessService processService;

    public Zone getById(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ZONE_NOT_FOUND));
    }

    /** processId 가 null 이면 전체 조회. */
    public Page<Zone> getZones(Long processId, Pageable pageable) {
        Pageable sorted = sortedByZoneCode(pageable);
        return processId == null
                ? zoneRepository.findAll(sorted)
                : zoneRepository.findAllByProcessId(processId, sorted);
    }

    /** 공정별 구역 목록. 공정이 없으면 404 를 내려주기 위해 존재 확인을 먼저 한다. */
    public Page<Zone> getZonesByProcessId(Long processId, Pageable pageable) {
        processService.getById(processId);
        return zoneRepository.findAllByProcessId(processId, sortedByZoneCode(pageable));
    }

    @Transactional
    public Zone create(ZoneCreateRequest request) {
        Process process = processService.getById(request.processId());

        if (zoneRepository.existsByProcessIdAndZoneCode(process.getId(), request.zoneCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ZONE_CODE);
        }

        Zone zone = zoneRepository.save(Zone.builder()
                .process(process)
                .zoneCode(request.zoneCode())
                .name(request.name())
                .mapX(request.mapX())
                .mapY(request.mapY())
                .build());

        log.info("구역 등록: zoneId={}, processId={}, zoneCode={}",
                zone.getId(), process.getId(), zone.getZoneCode());
        return zone;
    }

    @Transactional
    public Zone update(Long zoneId, ZoneUpdateRequest request) {
        Zone zone = getById(zoneId);

        // 공정 이동이 있으면 먼저 반영해야 중복 검사가 올바른 공정을 기준으로 이뤄진다.
        if (request.processId() != null && !request.processId().equals(zone.getProcess().getId())) {
            zone.moveTo(processService.getById(request.processId()));
        }

        String targetCode = request.zoneCode() != null ? request.zoneCode() : zone.getZoneCode();
        if (zoneRepository.existsByProcessIdAndZoneCodeAndIdNot(
                zone.getProcess().getId(), targetCode, zone.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ZONE_CODE);
        }

        zone.update(request.zoneCode(), request.name(), request.mapX(), request.mapY());
        return zone;
    }

    /**
     * 구역 삭제.
     *
     * <p>재직 중인 작업자가 남아 있으면 거부한다. 구역을 지우면 그 작업자들의 위치 정보가
     * 사라져 현장 인원 현황이 어긋난다. 작업자를 먼저 다른 구역으로 옮기거나 퇴사 처리해야 한다.
     */
    @Transactional
    public void delete(Long zoneId) {
        Zone zone = getById(zoneId);

        if (zoneRepository.countActiveWorkersOf(zoneId) > 0) {
            throw new BusinessException(ErrorCode.ZONE_HAS_WORKERS);
        }

        zoneRepository.delete(zone);
        log.info("구역 삭제: zoneId={}", zoneId);
    }

    /* ---------- 내부 ---------- */

    /** 구역은 코드 순으로 보는 편이 자연스럽다. (A-1, A-2, B-1 ...) */
    private Pageable sortedByZoneCode(Pageable pageable) {
        return PageableUtils.withDefaultSort(pageable, "zoneCode", Sort.Direction.ASC);
    }
}
