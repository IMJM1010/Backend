package com.example.be.domain.manager.controller;

import com.example.be.domain.manager.dto.request.ManagerCreateRequest;
import com.example.be.domain.manager.dto.request.ManagerUpdateRequest;
import com.example.be.domain.manager.dto.request.PasswordChangeRequest;
import com.example.be.domain.manager.dto.request.ProfileImageUpdateRequest;
import com.example.be.domain.manager.dto.response.ManagerResponse;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.service.ManagerService;
import com.example.be.global.common.ApiResponse;
import com.example.be.global.common.PageResponse;
import com.example.be.global.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 API.
 *
 * <p>인가 규칙이 두 군데로 나뉘어 있다.
 * <ul>
 *   <li>등록(POST) / 비활성화(DELETE) 의 ADMIN 검사 → {@code SecurityConfig}</li>
 *   <li>"본인 또는 ADMIN" 처럼 대상 리소스를 알아야 하는 검사 → {@code ManagerService}</li>
 * </ul>
 */
@Tag(name = "Managers", description = "관리자 API")
@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ManagerController {

    private final ManagerService managerService;

    @Operation(summary = "관리자 목록 조회", description = "role, isActive 로 필터링할 수 있다. 기본 정렬은 등록 최신순.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ManagerResponse>>> getManagers(
            @Parameter(description = "권한 필터") @RequestParam(required = false) ManagerRole role,
            @Parameter(description = "재직 여부 필터") @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(managerService.getManagers(role, isActive, pageable),
                        ManagerResponse::from)));
    }

    @Operation(summary = "관리자 상세 조회")
    @GetMapping("/{managerId}")
    public ResponseEntity<ApiResponse<ManagerResponse>> getManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(
                ManagerResponse.from(managerService.getById(managerId))));
    }

    @Operation(summary = "관리자 등록", description = "ADMIN 만 호출할 수 있다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ManagerResponse>> createManager(
            @Valid @RequestBody ManagerCreateRequest request) {

        ManagerResponse created = ManagerResponse.from(managerService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "관리자 정보 수정",
            description = "본인 또는 ADMIN 만 호출할 수 있다. 권한(role) 변경은 ADMIN 만 가능하며 본인 권한은 바꿀 수 없다.")
    @PatchMapping("/{managerId}")
    public ResponseEntity<ApiResponse<ManagerResponse>> updateManager(
            @PathVariable Long managerId,
            @Valid @RequestBody ManagerUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(ManagerResponse.from(
                managerService.update(managerId, request, SecurityUtils.getCurrentManagerId()))));
    }

    @Operation(summary = "비밀번호 변경", description = "본인만 호출할 수 있다. 변경 시 기존 세션은 끊긴다.")
    @PatchMapping("/{managerId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long managerId,
            @Valid @RequestBody PasswordChangeRequest request) {

        managerService.changePassword(managerId, request, SecurityUtils.getCurrentManagerId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @Operation(summary = "프로필 이미지 갱신",
            description = "이미지 URL 문자열을 받아 profile_image_url 을 갱신한다. 본인 또는 ADMIN 만 호출할 수 있다.")
    @PostMapping("/{managerId}/profile-image")
    public ResponseEntity<ApiResponse<ManagerResponse>> updateProfileImage(
            @PathVariable Long managerId,
            @Valid @RequestBody ProfileImageUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(ManagerResponse.from(
                managerService.updateProfileImage(
                        managerId, request.profileImageUrl(), SecurityUtils.getCurrentManagerId()))));
    }

    @Operation(summary = "관리자 비활성화",
            description = "실제로 삭제하지 않고 is_active 를 false 로 바꾼다. ADMIN 만 호출할 수 있으며 본인 계정은 불가.")
    @DeleteMapping("/{managerId}")
    public ResponseEntity<Void> deactivateManager(@PathVariable Long managerId) {
        managerService.deactivate(managerId, SecurityUtils.getCurrentManagerId());
        return ResponseEntity.noContent().build();
    }
}
