package com.example.be.domain.manager.dto.response;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;

import java.time.LocalDateTime;

/**
 * 관리자 정보 응답. 비밀번호와 refresh token 은 절대 포함하지 않는다.
 */
public record ManagerResponse(
        Long managerId,
        String loginId,
        String name,
        ManagerRole role,
        String profileImageUrl,
        LocalDateTime createdAt
) {

    public static ManagerResponse from(Manager manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getLoginId(),
                manager.getName(),
                manager.getRole(),
                manager.getProfileImageUrl(),
                manager.getCreatedAt()
        );
    }
}
