package com.example.be.domain.manager.service;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 도메인 서비스.
 *
 * <p>지금은 인증에 필요한 조회만 있다. 관리자 CRUD 는 다음 PR 에서 이 클래스에 추가한다.
 * 다른 도메인은 {@code ManagerRepository} 를 직접 쓰지 말고 이 서비스를 거칠 것.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;

    /** 없으면 예외. 조회 결과가 반드시 있어야 하는 곳에서 쓴다. */
    public Manager getById(Long managerId) {
        return managerRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MANAGER_NOT_FOUND));
    }

    /**
     * 로그인 아이디로 조회한다.
     *
     * <p>없을 때 MANAGER_NOT_FOUND 가 아니라 INVALID_CREDENTIALS 를 던지는 이유:
     * "없는 아이디"와 "틀린 비밀번호"를 구분해서 응답하면 공격자가 유효한 아이디를 추려낼 수 있다.
     */
    public Manager getByLoginIdForAuthentication(String loginId) {
        return managerRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }

    public boolean existsByLoginId(String loginId) {
        return managerRepository.existsByLoginId(loginId);
    }
}
