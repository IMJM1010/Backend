package com.example.be.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 발급 응답")
public record TokenResponse(

        @Schema(description = "API 호출에 사용할 Access Token")
        String accessToken,

        @Schema(description = "Access Token 만료 시 재발급에 사용할 Refresh Token")
        String refreshToken,

        @Schema(description = "인증 스킴. Authorization 헤더에 'Bearer {accessToken}' 형태로 넣는다", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 유효 기간 (초)", example = "1800")
        long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, BEARER, expiresIn);
    }
}
