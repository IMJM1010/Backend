package com.example.be.domain.manager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로필 이미지 갱신 요청.
 *
 * <p>지금은 파일 업로드가 아니라 이미지 URL 문자열만 받는다.
 * 나중에 S3 업로드로 바꾸더라도 이 엔드포인트의 응답 계약은 그대로 유지된다.
 */
@Schema(description = "프로필 이미지 갱신 요청")
public record ProfileImageUpdateRequest(

        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profiles/1.png")
        @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
        String profileImageUrl
) {
}
