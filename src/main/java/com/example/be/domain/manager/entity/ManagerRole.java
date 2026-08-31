package com.example.be.domain.manager.entity;

/**
 * 관리자 권한.
 *
 * <ul>
 *   <li>{@link #ADMIN} — 관리자 계정 관리, 공정·구역·센서 등 마스터 데이터 변경</li>
 *   <li>{@link #MANAGER} — 조회 전반, 알림 확인, 사고 처리, 긴급조치 실행, 본인 위젯 설정</li>
 * </ul>
 */
public enum ManagerRole {

    ADMIN,
    MANAGER;

    /** Spring Security 가 기대하는 권한 문자열. (hasRole("ADMIN") 이 ROLE_ADMIN 을 찾는다) */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
