package com.example.be.domain.auth.service;

import com.example.be.domain.auth.dto.request.LoginRequest;
import com.example.be.domain.auth.dto.response.TokenResponse;
import com.example.be.domain.manager.dto.response.ManagerResponse;
import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.service.ManagerService;
import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import com.example.be.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 흐름을 조율한다. 관리자 조회는 {@link ManagerService} 에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final ManagerService managerService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    /**
     * 로그인. 아이디/비밀번호를 확인하고 토큰 한 쌍을 발급한다.
     *
     * <p>발급된 refresh token 은 managers 에 저장한다. 로그아웃 시 이 값을 지워
     * "이미 로그아웃한 토큰으로 재발급" 을 막기 위함이다.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Manager manager = managerService.getByLoginIdForAuthentication(request.loginId());

        if (!passwordEncoder.matches(request.password(), manager.getPassword())) {
            // 어떤 단계에서 틀렸는지 응답으로 구분되지 않도록 같은 에러를 쓴다.
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 비밀번호 확인 이후에 검사한다. 먼저 검사하면 비밀번호를 몰라도
        // "비활성 계정" 응답만으로 해당 아이디가 존재한다는 사실이 드러난다.
        if (!manager.isActive()) {
            throw new BusinessException(ErrorCode.MANAGER_DEACTIVATED);
        }

        return issueTokens(manager);
    }

    /**
     * 로그아웃. 저장된 refresh token 을 제거한다.
     *
     * <p>이미 발급된 access token 은 만료 전까지 유효하다. 이는 무상태 JWT 의 특성이며,
     * access token 수명을 짧게(30분) 유지하는 것으로 노출 구간을 제한한다.
     */
    @Transactional
    public void logout(Long managerId) {
        Manager manager = managerService.getById(managerId);
        manager.clearRefreshToken();
        log.info("로그아웃: managerId={}", managerId);
    }

    /**
     * 토큰 재발급.
     *
     * <p>서명·만료·토큰 종류를 검증한 뒤, DB 에 저장된 값과 일치하는지까지 확인한다.
     * 서명만 검증하면 로그아웃한 토큰이나 이미 교체된 토큰도 통과하기 때문이다.
     *
     * <p>재발급 시 refresh token 도 함께 교체한다(rotation). 토큰이 유출되어도
     * 정상 사용자가 한 번 갱신하면 유출된 토큰은 더 이상 쓸 수 없게 된다.
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Jwt jwt = tokenProvider.parseRefreshToken(refreshToken);

        Long managerId;
        try {
            managerId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Manager manager = managerService.getById(managerId);
        if (!manager.matchesRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        // 토큰 발급 후에 비활성화됐을 수 있다. 재발급 시점에 다시 확인한다.
        if (!manager.isActive()) {
            throw new BusinessException(ErrorCode.MANAGER_DEACTIVATED);
        }

        return issueTokens(manager);
    }

    /** 로그인한 관리자 본인 정보. */
    public ManagerResponse getMyInfo(Long managerId) {
        return ManagerResponse.from(managerService.getById(managerId));
    }

    /* ---------- 내부 ---------- */

    private TokenResponse issueTokens(Manager manager) {
        String role = manager.getRole().name();
        String accessToken = tokenProvider.createAccessToken(manager.getId(), manager.getLoginId(), role);
        String refreshToken = tokenProvider.createRefreshToken(manager.getId(), manager.getLoginId(), role);

        LocalDateTime expiresAt = tokenProvider.calculateRefreshTokenExpiresAt();
        manager.updateRefreshToken(refreshToken, expiresAt);

        return TokenResponse.of(accessToken, refreshToken, tokenProvider.getAccessTokenValiditySeconds());
    }
}
