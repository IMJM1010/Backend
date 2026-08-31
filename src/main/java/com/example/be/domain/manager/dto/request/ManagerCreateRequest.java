package com.example.be.domain.manager.dto.request;

import com.example.be.domain.manager.entity.ManagerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 등록 요청")
public record ManagerCreateRequest(

        @Schema(description = "로그인 아이디", example = "manager01")
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4~50자여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "로그인 아이디는 영문, 숫자, - , _ 만 사용할 수 있습니다.")
        String loginId,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
        String password,

        @Schema(description = "관리자명", example = "김안전")
        @NotBlank(message = "관리자명은 필수입니다.")
        @Size(max = 30, message = "관리자명은 30자 이하여야 합니다.")
        String name,

        @Schema(description = "권한. 생략하면 MANAGER", example = "MANAGER")
        ManagerRole role,

        @Schema(description = "프로필 이미지 URL")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
        String profileImageUrl
) {
}
