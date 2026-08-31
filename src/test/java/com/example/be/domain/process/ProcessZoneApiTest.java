package com.example.be.domain.process;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.entity.ProcessStatus;
import com.example.be.domain.process.repository.ProcessRepository;
import com.example.be.domain.zone.entity.Zone;
import com.example.be.domain.zone.repository.ZoneRepository;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("공정 · 구역 API")
class ProcessZoneApiTest {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private ProcessRepository processRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;
    private Process hubProcess;

    @BeforeEach
    void setUp() throws Exception {
        saveManager("admin01", ManagerRole.ADMIN);
        saveManager("member01", ManagerRole.MANAGER);
        adminToken = login("admin01");
        memberToken = login("member01");

        hubProcess = processRepository.save(Process.builder().name("허브공정").build());
    }

    /* ---------- 공정 ---------- */

    @Test
    @DisplayName("ADMIN 은 공정을 등록할 수 있고 기본 상태는 RUNNING 이다")
    void createProcess() throws Exception {
        mockMvc.perform(post("/api/processes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"조립공정"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("조립공정"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("MANAGER 는 공정을 등록할 수 없다")
    void createProcess_byManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/processes")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"조립공정"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("MANAGER 도 공정 목록은 조회할 수 있다")
    void listProcess_byManager() throws Exception {
        mockMvc.perform(get("/api/processes").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("status 로 공정을 필터링할 수 있다")
    void listProcess_filterByStatus() throws Exception {
        processRepository.save(Process.builder().name("중단공정").status(ProcessStatus.STOPPED).build());

        mockMvc.perform(get("/api/processes")
                        .param("status", "STOPPED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("중단공정"));
    }

    @Test
    @DisplayName("없는 공정을 조회하면 404 를 반환한다")
    void getProcess_notFound() throws Exception {
        mockMvc.perform(get("/api/processes/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    @DisplayName("공정 상태를 STOPPED 로 바꿀 수 있다")
    void updateProcess() throws Exception {
        mockMvc.perform(patch("/api/processes/" + hubProcess.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"STOPPED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STOPPED"))
                .andExpect(jsonPath("$.data.name").value("허브공정"));
    }

    @Test
    @DisplayName("하위 구역이 없으면 공정을 삭제할 수 있다")
    void deleteProcess_withoutZones() throws Exception {
        mockMvc.perform(delete("/api/processes/" + hubProcess.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("하위 구역이 남아 있으면 공정을 삭제할 수 없다")
    void deleteProcess_withZones_rejected() throws Exception {
        saveZone(hubProcess, "A-1");

        mockMvc.perform(delete("/api/processes/" + hubProcess.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROCESS_HAS_ZONES"));
    }

    /* ---------- 구역 ---------- */

    @Test
    @DisplayName("구역을 등록하면 좌표와 공정명이 함께 내려온다")
    void createZone() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processId":%d,"zoneCode":"A-1","name":"자재 적치장","mapX":120.50,"mapY":84.25}"""
                                .formatted(hubProcess.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.zoneCode").value("A-1"))
                .andExpect(jsonPath("$.data.processName").value("허브공정"))
                .andExpect(jsonPath("$.data.mapX").value(120.50));
    }

    @Test
    @DisplayName("같은 공정 안에서 구역 코드는 중복될 수 없다")
    void createZone_duplicateCodeInSameProcess() throws Exception {
        saveZone(hubProcess, "A-1");

        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processId":%d,"zoneCode":"A-1"}""".formatted(hubProcess.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ZONE_CODE"));
    }

    @Test
    @DisplayName("다른 공정이라면 같은 구역 코드를 쓸 수 있다")
    void createZone_sameCodeInOtherProcess() throws Exception {
        saveZone(hubProcess, "A-1");
        Process other = processRepository.save(Process.builder().name("조립공정").build());

        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processId":%d,"zoneCode":"A-1"}""".formatted(other.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("없는 공정에 구역을 등록하면 404 를 반환한다")
    void createZone_unknownProcess() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processId":99999,"zoneCode":"A-1"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    @DisplayName("processId 가 없으면 400 과 필드별 상세를 반환한다")
    void createZone_validationFailure() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneCode":"A-1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("processId"));
    }

    @Test
    @DisplayName("구역을 다른 공정으로 옮길 수 있다")
    void updateZone_moveToOtherProcess() throws Exception {
        Zone zone = saveZone(hubProcess, "A-1");
        Process other = processRepository.save(Process.builder().name("조립공정").build());

        mockMvc.perform(patch("/api/zones/" + zone.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processId":%d}""".formatted(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processName").value("조립공정"));
    }

    @Test
    @DisplayName("공정별 구역 목록을 조회할 수 있다")
    void getZonesByProcess() throws Exception {
        saveZone(hubProcess, "A-2");
        saveZone(hubProcess, "A-1");
        Process other = processRepository.save(Process.builder().name("조립공정").build());
        saveZone(other, "B-1");

        mockMvc.perform(get("/api/processes/" + hubProcess.getId() + "/zones")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                // 기본 정렬은 구역 코드 오름차순
                .andExpect(jsonPath("$.data.content[0].zoneCode").value("A-1"));
    }

    @Test
    @DisplayName("MANAGER 는 구역을 삭제할 수 없다")
    void deleteZone_byManager_forbidden() throws Exception {
        Zone zone = saveZone(hubProcess, "A-1");

        mockMvc.perform(delete("/api/zones/" + zone.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    /* ---------- 헬퍼 ---------- */

    private void saveManager(String loginId, ManagerRole role) {
        managerRepository.save(Manager.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(PASSWORD))
                .name(loginId)
                .role(role)
                .build());
    }

    private Zone saveZone(Process process, String zoneCode) {
        return zoneRepository.save(Zone.builder()
                .process(process)
                .zoneCode(zoneCode)
                .name(zoneCode + " 구역")
                .mapX(new BigDecimal("10.00"))
                .mapY(new BigDecimal("20.00"))
                .build());
    }

    private String login(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}""".formatted(loginId, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = body.indexOf(marker) + marker.length();
        return body.substring(start, body.indexOf('"', start));
    }
}
