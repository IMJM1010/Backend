package com.example.be.domain.auth.controller;

import com.example.be.domain.auth.dto.request.LoginRequest;
import com.example.be.domain.auth.dto.request.TokenRefreshRequest;
import com.example.be.domain.auth.dto.response.TokenResponse;
import com.example.be.domain.auth.service.AuthService;
import com.example.be.domain.manager.dto.response.ManagerResponse;
import com.example.be.global.common.ApiResponse;
import com.example.be.global.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API.
 *
 * <p>주의: Swagger 응답 애노테이션({@code io.swagger.v3.oas.annotations.responses.ApiResponse})은
 * 우리 {@link ApiResponse} 와 이름이 같으므로 이 클래스에서는 import 하지 않는다.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "login_id 와 password 로 인증하고 JWT 토큰을 발급받는다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(
            summary = "로그아웃",
            description = "서버에 저장된 Refresh Token 을 제거한다. "
                    + "이미 발급된 Access Token 은 만료 전까지 유효하므로 클라이언트에서도 폐기해야 한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(SecurityUtils.getCurrentManagerId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token 으로 Access Token 을 재발급한다. Refresh Token 도 함께 교체된다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request.refreshToken())));
    }

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 관리자 본인의 정보를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ManagerResponse>> getMyInfo() {
        return ResponseEntity.ok(ApiResponse.success(
                authService.getMyInfo(SecurityUtils.getCurrentManagerId())));
    }
}
