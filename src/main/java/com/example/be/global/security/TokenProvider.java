package com.example.be.global.security;

import com.example.be.global.exception.BusinessException;
import com.example.be.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Access / Refresh 토큰 발급과 Refresh 토큰 검증을 담당한다.
 *
 * <p>Access 토큰의 검증은 이 클래스가 하지 않는다.
 * Spring Security 의 리소스 서버 필터가 {@link JwtDecoder} 로 자동 처리한다.
 */
@Slf4j
@Component
public class TokenProvider {

    public static final String CLAIM_LOGIN_ID = "loginId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_TYPE = "type";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final String issuer;
    private final long accessTokenValidityMillis;
    private final long refreshTokenValidityMillis;

    public TokenProvider(JwtEncoder jwtEncoder,
                         JwtDecoder jwtDecoder,
                         @Value("${jwt.issuer}") String issuer,
                         @Value("${jwt.access-token-validity}") long accessTokenValidityMillis,
                         @Value("${jwt.refresh-token-validity}") long refreshTokenValidityMillis) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.issuer = issuer;
        this.accessTokenValidityMillis = accessTokenValidityMillis;
        this.refreshTokenValidityMillis = refreshTokenValidityMillis;
    }

    /* ---------- 발급 ---------- */

    public String createAccessToken(Long managerId, String loginId, String role) {
        return createToken(managerId, loginId, role, TOKEN_TYPE_ACCESS, accessTokenValidityMillis);
    }

    public String createRefreshToken(Long managerId, String loginId, String role) {
        return createToken(managerId, loginId, role, TOKEN_TYPE_REFRESH, refreshTokenValidityMillis);
    }

    private String createToken(Long managerId, String loginId, String role,
                               String tokenType, long validityMillis) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusMillis(validityMillis))
                // subject 에 관리자 식별자를 담는다. 인증 후 SecurityUtils 가 여기서 읽는다.
                .subject(String.valueOf(managerId))
                .claim(CLAIM_LOGIN_ID, loginId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /* ---------- 검증 ---------- */

    /**
     * Refresh 토큰을 검증하고 파싱한다.
     *
     * <p>Access 토큰을 refresh 엔드포인트에 넣어 무한 갱신하는 것을 막기 위해
     * {@code type} 클레임까지 확인한다.
     */
    public Jwt parseRefreshToken(String token) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException e) {
            log.warn("Refresh token 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!TOKEN_TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN_TYPE);
        }
        return jwt;
    }

    /* ---------- 부가 정보 ---------- */

    public LocalDateTime calculateRefreshTokenExpiresAt() {
        return LocalDateTime.ofInstant(
                Instant.now().plusMillis(refreshTokenValidityMillis), ZoneId.systemDefault());
    }

    /** 프론트가 재발급 시점을 계산할 수 있도록 응답에 담아준다. (초 단위) */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMillis / 1000;
    }
}
