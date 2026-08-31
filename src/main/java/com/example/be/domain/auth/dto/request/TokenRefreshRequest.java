package com.example.be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record TokenRefreshRequest(

        @Schema(description = "로그인 시 발급받은 Refresh Token")
        @NotBlank(message = "Refresh Token 은 필수입니다.")
        String refreshToken
) {
}
