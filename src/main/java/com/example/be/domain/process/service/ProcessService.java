package com.example.be.domain.process.service;

import com.example.be.domain.process.dto.request.ProcessCreateRequest;
import com.example.be.domain.process.dto.request.ProcessUpdateRequest;
import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.entity.ProcessStatus;
import com.example.be.domain.process.repository.ProcessRepository;
import com.example.be.global.common.PageableUtils;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공정 도메인 서비스.
 *
 * <p>다른 도메인은 {@code ProcessRepository} 를 직접 쓰지 말고 이 서비스를 거칠 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessService {

    private final ProcessRepository processRepository;

    public Process getById(Long processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_NOT_FOUND));
    }

    /** status 가 null 이면 전체 조회. */
    public Page<Process> getProcesses(ProcessStatus status, Pageable pageable) {
        Pageable sorted = PageableUtils.withLatestFirst(pageable);
        return status == null
                ? processRepository.findAll(sorted)
                : processRepository.findAllByStatus(status, sorted);
    }

    @Transactional
    public Process create(ProcessCreateRequest request) {
        Process process = processRepository.save(Process.builder()
                .name(request.name())
                .status(request.status())
                .build());

        log.info("공정 등록: processId={}, name={}", process.getId(), process.getName());
        return process;
    }

    @Transactional
    public Process update(Long processId, ProcessUpdateRequest request) {
        Process process = getById(processId);
        process.update(request.name(), request.status());
        return process;
    }

    /**
     * 공정 삭제.
     *
     * <p>하위 구역이 남아 있으면 거부한다. 구역이 사라지면 그 구역에 매인 작업자·센서·알림·사고가
     * 전부 고아가 되므로, 구역을 먼저 정리하도록 강제한다.
     */
    @Transactional
    public void delete(Long processId) {
        Process process = getById(processId);

        if (processRepository.countZonesOf(processId) > 0) {
            throw new BusinessException(ErrorCode.PROCESS_HAS_ZONES);
        }

        processRepository.delete(process);
        log.info("공정 삭제: processId={}", processId);
    }
}
