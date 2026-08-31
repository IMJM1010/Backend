package com.example.be.domain.manager.dto.request;

import com.example.be.domain.manager.entity.ManagerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 관리자 정보 수정 요청. PATCH 이므로 null 인 필드는 변경하지 않는다.
 *
 * <p>{@code role} 은 ADMIN 만 변경할 수 있고, 본인 권한은 스스로 바꿀 수 없다.
 */
@Schema(description = "관리자 정보 수정 요청 (null 인 필드는 변경하지 않음)")
public record ManagerUpdateRequest(

        @Schema(description = "관리자명", example = "김안전")
        @Size(max = 30, message = "관리자명은 30자 이하여야 합니다.")
        String name,

        @Schema(description = "권한. ADMIN 만 변경 가능", example = "ADMIN")
        ManagerRole role,

        @Schema(description = "프로필 이미지 URL")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
        String profileImageUrl
) {
}
