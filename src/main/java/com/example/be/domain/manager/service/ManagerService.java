package com.example.be.domain.manager.service;

import com.example.be.domain.manager.dto.request.ManagerCreateRequest;
import com.example.be.domain.manager.dto.request.ManagerUpdateRequest;
import com.example.be.domain.manager.dto.request.PasswordChangeRequest;
import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.global.common.PageableUtils;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 도메인 서비스.
 *
 * <p>다른 도메인은 {@code ManagerRepository} 를 직접 쓰지 말고 이 서비스를 거칠 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    /* ---------- 조회 ---------- */

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

    /**
     * 관리자 목록. role / isActive 로 필터링한다. 둘 다 null 이면 전체 조회.
     *
     * <p>JPQL 의 {@code :param is null} 방식 대신 Specification 을 쓰는 이유는
     * enum 파라미터에 null 을 넘길 때의 타입 추론 문제를 피하기 위함이다.
     */
    public Page<Manager> getManagers(ManagerRole role, Boolean isActive, Pageable pageable) {
        Specification<Manager> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return managerRepository.findAll(spec, PageableUtils.withLatestFirst(pageable));
    }

    /* ---------- 생성 / 수정 ---------- */

    /**
     * 관리자 등록. 호출 권한(ADMIN) 검사는 SecurityConfig 에서 이미 끝난 상태로 들어온다.
     */
    @Transactional
    public Manager create(ManagerCreateRequest request) {
        if (managerRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        Manager manager = managerRepository.save(Manager.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(request.role())
                .profileImageUrl(request.profileImageUrl())
                .build());

        log.info("관리자 등록: managerId={}, loginId={}", manager.getId(), manager.getLoginId());
        return manager;
    }

    /**
     * 관리자 정보 수정.
     *
     * <p>권한 규칙 두 가지를 여기서 강제한다.
     * <ul>
     *   <li>본인 또는 ADMIN 만 수정할 수 있다.</li>
     *   <li>권한(role) 변경은 ADMIN 만 가능하며, <b>본인 권한은 스스로 바꿀 수 없다.</b>
     *       그렇지 않으면 MANAGER 가 자기 계정을 ADMIN 으로 승격시킬 수 있다.</li>
     * </ul>
     */
    @Transactional
    public Manager update(Long targetId, ManagerUpdateRequest request, Long requesterId) {
        Manager requester = getById(requesterId);
        Manager target = getById(targetId);
        verifySelfOrAdmin(target, requester);

        if (request.role() != null && request.role() != target.getRole()) {
            if (!requester.isAdmin()) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            if (target.getId().equals(requester.getId())) {
                throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
            }
        }

        target.updateProfile(request.name(), request.role(), request.profileImageUrl());
        return target;
    }

    /**
     * 비밀번호 변경.
     *
     * <p>ADMIN 이라도 남의 현재 비밀번호는 모르므로, 이 API 는 본인만 사용할 수 있게 한다.
     * 관리자가 타인의 비밀번호를 초기화해야 한다면 별도의 초기화 API 를 만드는 것이 맞다.
     */
    @Transactional
    public void changePassword(Long targetId, PasswordChangeRequest request, Long requesterId) {
        if (!targetId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Manager manager = getById(targetId);

        if (!passwordEncoder.matches(request.currentPassword(), manager.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(request.newPassword(), manager.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        // changePassword 안에서 refresh token 도 지운다. 비밀번호를 바꾸면 기존 세션은 끊는다.
        manager.changePassword(passwordEncoder.encode(request.newPassword()));
        log.info("비밀번호 변경: managerId={}", targetId);
    }

    @Transactional
    public Manager updateProfileImage(Long targetId, String profileImageUrl, Long requesterId) {
        Manager requester = getById(requesterId);
        Manager target = getById(targetId);
        verifySelfOrAdmin(target, requester);

        target.updateProfileImageUrl(profileImageUrl);
        return target;
    }

    /**
     * 관리자 비활성화. 실제 행을 지우지 않는다.
     *
     * <p>사고 처리자·알림 확인자·긴급조치 실행자로 이름이 남아 있어 삭제하면 이력이 깨진다.
     * 본인 계정은 비활성화할 수 없다. 마지막 ADMIN 이 스스로를 잠가버리는 것을 막는다.
     */
    @Transactional
    public void deactivate(Long targetId, Long requesterId) {
        if (targetId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.CANNOT_DEACTIVATE_SELF);
        }

        Manager target = getById(targetId);
        if (!target.isActive()) {
            throw new BusinessException(ErrorCode.MANAGER_ALREADY_DEACTIVATED);
        }

        target.deactivate();
        log.info("관리자 비활성화: managerId={}, 요청자={}", targetId, requesterId);
    }

    /* ---------- 내부 ---------- */

    private void verifySelfOrAdmin(Manager target, Manager requester) {
        if (requester.isAdmin() || target.getId().equals(requester.getId())) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

}
