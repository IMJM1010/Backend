package com.example.be.domain.manager;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("관리자 API")
class ManagerApiTest {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Manager admin;
    private Manager member;
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        admin = save("admin01", "관리자", ManagerRole.ADMIN);
        member = save("member01", "일반매니저", ManagerRole.MANAGER);
        adminToken = login("admin01");
        memberToken = login("member01");
    }

    /* ---------- 등록 ---------- */

    @Test
    @DisplayName("ADMIN 은 관리자를 등록할 수 있다")
    void create_byAdmin() throws Exception {
        mockMvc.perform(post("/api/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"newbie01","password":"password123!","name":"신입","role":"MANAGER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.loginId").value("newbie01"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("MANAGER 는 관리자를 등록할 수 없다")
    void create_byManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/managers")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"newbie02","password":"password123!","name":"신입"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("이미 사용 중인 로그인 아이디는 409 를 반환한다")
    void create_duplicateLoginId() throws Exception {
        mockMvc.perform(post("/api/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"member01","password":"password123!","name":"중복"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    /* ---------- 조회 ---------- */

    @Test
    @DisplayName("목록을 role 로 필터링할 수 있다")
    void list_filterByRole() throws Exception {
        mockMvc.perform(get("/api/managers")
                        .param("role", "ADMIN")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("응답에 비밀번호가 포함되지 않는다")
    void detail_hidesPassword() throws Exception {
        mockMvc.perform(get("/api/managers/" + member.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    /* ---------- 수정 ---------- */

    @Test
    @DisplayName("본인 정보는 수정할 수 있다")
    void update_self() throws Exception {
        mockMvc.perform(patch("/api/managers/" + member.getId())
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"이름변경"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이름변경"));
    }

    @Test
    @DisplayName("MANAGER 는 다른 관리자의 정보를 수정할 수 없다")
    void update_other_forbidden() throws Exception {
        mockMvc.perform(patch("/api/managers/" + admin.getId())
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"몰래변경"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("MANAGER 는 스스로를 ADMIN 으로 승격시킬 수 없다")
    void update_selfPromotion_forbidden() throws Exception {
        mockMvc.perform(patch("/api/managers/" + member.getId())
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("ADMIN 도 본인 권한은 변경할 수 없다")
    void update_adminOwnRole_forbidden() throws Exception {
        mockMvc.perform(patch("/api/managers/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"MANAGER"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_ROLE"));
    }

    /* ---------- 비밀번호 ---------- */

    @Test
    @DisplayName("본인 비밀번호를 변경할 수 있고, 변경 후 기존 Refresh Token 은 무효가 된다")
    void changePassword_success() throws Exception {
        String refreshToken = loginAndExtract("member01", "refreshToken");

        mockMvc.perform(patch("/api/managers/" + member.getId() + "/password")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123!","newPassword":"newPassword456!"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISMATCH"));
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 400 을 반환한다")
    void changePassword_wrongCurrent() throws Exception {
        mockMvc.perform(patch("/api/managers/" + member.getId() + "/password")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong!!","newPassword":"newPassword456!"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("남의 비밀번호는 ADMIN 이라도 변경할 수 없다")
    void changePassword_other_forbidden() throws Exception {
        mockMvc.perform(patch("/api/managers/" + member.getId() + "/password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123!","newPassword":"newPassword456!"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    /* ---------- 프로필 이미지 ---------- */

    @Test
    @DisplayName("프로필 이미지 URL 을 갱신할 수 있다")
    void updateProfileImage() throws Exception {
        mockMvc.perform(post("/api/managers/" + member.getId() + "/profile-image")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileImageUrl":"https://cdn.example.com/p/1.png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.example.com/p/1.png"));
    }

    /* ---------- 비활성화 ---------- */

    @Test
    @DisplayName("비활성화된 관리자는 로그인할 수 없다")
    void deactivate_blocksLogin() throws Exception {
        mockMvc.perform(delete("/api/managers/" + member.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("member01")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MANAGER_DEACTIVATED"));
    }

    @Test
    @DisplayName("본인 계정은 비활성화할 수 없다")
    void deactivate_self_rejected() throws Exception {
        mockMvc.perform(delete("/api/managers/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));
    }

    /* ---------- 헬퍼 ---------- */

    private Manager save(String loginId, String name, ManagerRole role) {
        return managerRepository.save(Manager.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(PASSWORD))
                .name(name)
                .role(role)
                .build());
    }

    private String loginBody(String loginId) {
        return """
                {"loginId":"%s","password":"%s"}""".formatted(loginId, PASSWORD);
    }

    private String login(String loginId) throws Exception {
        return loginAndExtract(loginId, "accessToken");
    }

    private String loginAndExtract(String loginId, String field) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(loginId)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String marker = "\"%s\":\"".formatted(field);
        int start = body.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("응답에 %s 가 없습니다: %s".formatted(field, body));
        }
        start += marker.length();
        return body.substring(start, body.indexOf('"', start));
    }
}
