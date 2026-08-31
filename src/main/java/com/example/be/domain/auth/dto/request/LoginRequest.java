package com.example.be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(

        @Schema(description = "로그인 아이디", example = "admin01")
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        String loginId,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
