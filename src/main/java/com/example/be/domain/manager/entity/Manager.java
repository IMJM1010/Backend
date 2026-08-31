package com.example.be.domain.manager.entity;

import com.example.be.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자. 이 서비스의 유일한 로그인 주체다. (작업자는 관리 대상일 뿐 로그인하지 않는다)
 */
@Entity
@Table(
        name = "managers",
        uniqueConstraints = @UniqueConstraint(name = "uk_managers_login_id", columnNames = "login_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manager extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id")
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    /** BCrypt 해시. 평문 저장 금지. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ManagerRole role;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /*
     * 아래 두 컬럼은 ERD 에 없는 확장 컬럼이다.
     * 명세의 "로그아웃 = 토큰 무효화" 를 지키려면 서버가 유효한 refresh token 을 알고 있어야 하는데,
     * 순수 무상태 JWT 로는 발급된 토큰을 되돌릴 방법이 없다. 별도 테이블이나 Redis 를 두는 대신
     * 관리자 1인당 1세션을 전제로 managers 에 직접 보관한다.
     * ※ ERDCloud 문서에도 반영할 것.
     */
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Builder
    private Manager(String loginId, String password, String name,
                    ManagerRole role, String profileImageUrl) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role != null ? role : ManagerRole.MANAGER;
        this.profileImageUrl = profileImageUrl;
    }

    /* ---------- 상태 변경 ---------- */

    /** 로그인 성공 또는 토큰 재발급 시 호출한다. */
    public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = expiresAt;
    }

    /** 로그아웃. 저장된 refresh token 을 지워 재발급을 막는다. */
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    /**
     * 클라이언트가 제시한 refresh token 이 서버에 저장된 것과 같은지 확인한다.
     * 로그아웃 후의 토큰이나 탈취되어 이미 교체된 토큰을 걸러낸다.
     */
    public boolean matchesRefreshToken(String candidate) {
        return this.refreshToken != null && this.refreshToken.equals(candidate);
    }

    /** 이미 인코딩된 비밀번호를 받는다. 평문을 넘기지 말 것. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        // 비밀번호가 바뀌면 기존 세션은 끊는다.
        clearRefreshToken();
    }

    public void updateProfile(String name, ManagerRole role, String profileImageUrl) {
        if (name != null) {
            this.name = name;
        }
        if (role != null) {
            this.role = role;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
