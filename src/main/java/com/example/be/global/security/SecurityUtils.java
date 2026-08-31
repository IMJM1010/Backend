package com.example.be.global.security;

import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 현재 인증된 관리자 정보를 꺼내는 헬퍼.
 *
 * <p>컨트롤러에서 {@code @AuthenticationPrincipal Jwt jwt} 를 받아 직접 파싱하는 대신
 * 이걸 쓰면 subject 파싱 규칙이 한 곳에 모인다.
 *
 * <pre>Long managerId = SecurityUtils.getCurrentManagerId();</pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 인증되지 않았으면 UNAUTHORIZED 예외. */
    public static Long getCurrentManagerId() {
        Jwt jwt = getCurrentJwt();
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            // 우리가 발급한 토큰이라면 subject 는 항상 관리자 식별자다.
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public static String getCurrentLoginId() {
        return getCurrentJwt().getClaimAsString(TokenProvider.CLAIM_LOGIN_ID);
    }

    public static String getCurrentRole() {
        return getCurrentJwt().getClaimAsString(TokenProvider.CLAIM_ROLE);
    }

    private static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return jwt;
    }
}
