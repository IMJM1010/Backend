package com.example.be.domain.auth;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 흐름 통합 테스트.
 *
 * <p>SecurityConfig, JwtConfig, TokenProvider, 리소스 서버 필터가 실제로 맞물려
 * 동작하는지를 HTTP 레벨에서 확인한다. 설정이 어긋나면 여기서 먼저 깨진다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("인증 API")
class AuthApiTest {

    private static final String LOGIN_ID = "tester01";
    private static final String RAW_PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        managerRepository.save(Manager.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .name("테스터")
                .role(ManagerRole.MANAGER)
                .build());
    }

    @Test
    @DisplayName("올바른 아이디와 비밀번호로 로그인하면 토큰 한 쌍을 발급받는다")
    void login_success() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(LOGIN_ID, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 과 INVALID_CREDENTIALS 를 반환한다")
    void login_wrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(LOGIN_ID, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("없는 아이디도 존재 여부가 드러나지 않도록 같은 에러를 반환한다")
    void login_unknownLoginId() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no-such-user", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("아이디가 비어 있으면 400 과 필드별 상세를 반환한다")
    void login_validationFailure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", RAW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("loginId"));
    }

    @Test
    @DisplayName("토큰 없이 보호된 API 를 호출하면 401 을 반환한다")
    void me_withoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("발급받은 Access Token 으로 내 정보를 조회할 수 있다")
    void me_withToken() throws Exception {
        String accessToken = loginAndExtract("accessToken");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value(LOGIN_ID))
                .andExpect(jsonPath("$.data.role").value("MANAGER"))
                // 비밀번호가 응답에 새어 나오지 않아야 한다.
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("Refresh Token 으로 토큰을 재발급받을 수 있다")
    void refresh_success() throws Exception {
        String refreshToken = loginAndExtract("refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Access Token 을 재발급에 사용하면 거부된다")
    void refresh_withAccessToken() throws Exception {
        String accessToken = loginAndExtract("accessToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_TYPE"));
    }

    @Test
    @DisplayName("로그아웃하면 기존 Refresh Token 으로 재발급할 수 없다")
    void logout_invalidatesRefreshToken() throws Exception {
        String accessToken = loginAndExtract("accessToken");
        String refreshToken = loginAndExtract("refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISMATCH"));
    }

    /* ---------- 헬퍼 ---------- */

    private String loginBody(String loginId, String password) {
        return """
                {"loginId":"%s","password":"%s"}""".formatted(loginId, password);
    }

    private String loginAndExtract(String field) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(LOGIN_ID, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = extractJsonString(body, field);
        if (token.isBlank()) {
            throw new AssertionError("%s 가 비어 있습니다: %s".formatted(field, body));
        }
        return token;
    }

    /** 테스트 응답에서 문자열 필드를 뽑는다. JSON 매퍼 빈 타입에 의존하지 않으려고 직접 파싱한다. */
    private String extractJsonString(String json, String field) {
        String marker = "\"%s\":\"".formatted(field);
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("응답에 %s 필드가 없습니다: %s".formatted(field, json));
        }
        start += marker.length();
        return json.substring(start, json.indexOf('"', start));
    }
}
